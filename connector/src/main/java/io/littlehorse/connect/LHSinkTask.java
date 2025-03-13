package io.littlehorse.connect;

import io.littlehorse.sdk.common.config.LHConfig;
import io.littlehorse.sdk.common.proto.LittleHorseGrpc.LittleHorseBlockingStub;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Getter
public abstract class LHSinkTask extends SinkTask {

    private final Map<TopicPartition, OffsetAndMetadata> successfulOffsets = new HashMap<>();
    private LHSinkConnectorConfig connectorConfig;
    private LittleHorseBlockingStub blockingStub;
    private LHConfig lhConfig;

    @Override
    public String version() {
        return LHSinkConnectorVersion.version();
    }

    public abstract LHSinkConnectorConfig initializeConfig(Map<String, String> props);

    @Override
    public void start(Map<String, String> props) {
        connectorConfig = initializeConfig(props);
        lhConfig = connectorConfig.toLHConfig();
        blockingStub = lhConfig.getBlockingStub();
        log.debug(
                "Starting tasks {} ({}) with DLQ {}",
                connectorConfig.getConnectorName(),
                getClass().getSimpleName(),
                isDLQEnabled() ? "enabled" : "disabled");
    }

    @Override
    public void stop() {
        log.debug("Stopping {}", getClass().getSimpleName());
    }

    @Override
    public void put(Collection<SinkRecord> records) {
        records.forEach(sinkRecord -> {
            log.debug(
                    "Processing record [topic={}, partition={}, offset={}]",
                    sinkRecord.topic(),
                    sinkRecord.kafkaPartition(),
                    sinkRecord.kafkaOffset());

            try {
                executeGrpcCall(
                        new IdempotentSinkRecord(calculateIdempotencyKey(sinkRecord), sinkRecord));
                updateSuccessfulOffsets(sinkRecord);
            } catch (Exception e) {
                log.error("Error processing record in task {}", getClass().getSimpleName(), e);

                if (!doesTolerateErrors()) {
                    // full stop
                    throw e;
                }

                // send error to the dlq
                report(sinkRecord, e);
                updateSuccessfulOffsets(sinkRecord);
            }
        });
    }

    private void updateSuccessfulOffsets(SinkRecord sinkRecord) {
        successfulOffsets.put(
                new TopicPartition(sinkRecord.topic(), sinkRecord.kafkaPartition()),
                new OffsetAndMetadata(sinkRecord.kafkaOffset() + 1));
    }

    private String calculateIdempotencyKey(SinkRecord sinkRecord) {
        // to ensure idempotency we use: connector name + topic + partition + offset
        return String.format(
                "%s-%s-%d-%d",
                connectorConfig.getConnectorName(),
                sinkRecord.topic(),
                sinkRecord.kafkaPartition(),
                sinkRecord.kafkaOffset());
    }

    public abstract void executeGrpcCall(IdempotentSinkRecord sinkRecord);

    private void report(SinkRecord sinkRecord, Exception e) {
        if (!isDLQEnabled()) return;

        log.warn(
                "Reporting error [topic={}, partition={}, offset={}]",
                sinkRecord.topic(),
                sinkRecord.kafkaPartition(),
                sinkRecord.kafkaOffset(),
                e);

        context.errantRecordReporter().report(sinkRecord, e);
    }

    private boolean doesTolerateErrors() {
        return connectorConfig
                .getErrorsTolerance()
                .equals(LHSinkConnectorConfig.ERRORS_TOLERANCE_ALL);
    }

    private boolean isDLQEnabled() {
        try {
            return context.errantRecordReporter() != null;
        } catch (NoSuchMethodError | NoClassDefFoundError e) {
            return false;
        }
    }

    @Override
    public Map<TopicPartition, OffsetAndMetadata> preCommit(
            Map<TopicPartition, OffsetAndMetadata> currentOffsets) {
        log.debug("Commiting partitions {}", successfulOffsets);
        return successfulOffsets;
    }

    @Override
    public void open(Collection<TopicPartition> partitions) {
        log.debug("Opening task {} with partitions {}", getClass().getSimpleName(), partitions);
    }

    @Override
    public void close(Collection<TopicPartition> partitions) {
        log.debug("Closing task {} with partitions {}", getClass().getSimpleName(), partitions);
        // clean offset cache
        successfulOffsets.clear();
    }
}
