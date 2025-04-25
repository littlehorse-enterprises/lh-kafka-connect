package io.littlehorse.connect;

import static org.apache.kafka.connect.runtime.ConnectorConfig.ERRORS_TOLERANCE_CONFIG;
import static org.apache.kafka.connect.runtime.ConnectorConfig.NAME_CONFIG;

import io.littlehorse.connect.record.IdempotentSinkRecord;
import io.littlehorse.connect.util.VersionReader;
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
    private String connectorName;
    private String errorsTolerance;

    public abstract void executeGrpcCall(IdempotentSinkRecord sinkRecord);

    public abstract LHSinkConnectorConfig configure(Map<String, String> props);

    @Override
    public String version() {
        return VersionReader.version();
    }

    @Override
    public void start(Map<String, String> props) {
        connectorName = props.get(NAME_CONFIG);
        errorsTolerance = props.getOrDefault(ERRORS_TOLERANCE_CONFIG, "none");
        connectorConfig = configure(props);
        lhConfig = connectorConfig.toLHConfig();
        blockingStub = lhConfig.getBlockingStub();
        log.debug(
                "Starting tasks {}[{}] with DLQ {}",
                getClass().getSimpleName(),
                connectorName,
                isDLQEnabled() ? "enabled" : "disabled");
    }

    @Override
    public void stop() {
        log.debug("Stopping {}[{}]", getClass().getSimpleName(), connectorName);
    }

    @Override
    public void put(Collection<SinkRecord> records) {
        records.forEach(sinkRecord -> {
            log.debug(
                    "Processing record [topic={}, partition={}, offset={}] for task {}[{}]",
                    sinkRecord.topic(),
                    sinkRecord.kafkaPartition(),
                    sinkRecord.kafkaOffset(),
                    getClass().getSimpleName(),
                    connectorName);

            try {
                executeGrpcCall(
                        new IdempotentSinkRecord(calculateIdempotencyKey(sinkRecord), sinkRecord));
                updateSuccessfulOffsets(sinkRecord);
            } catch (Exception e) {
                log.error(
                        "Error processing record for task {}[{}]",
                        getClass().getSimpleName(),
                        connectorName,
                        e);

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

    @Override
    public Map<TopicPartition, OffsetAndMetadata> preCommit(
            Map<TopicPartition, OffsetAndMetadata> currentOffsets) {
        log.debug(
                "Commiting partitions {} for {}[{}]",
                successfulOffsets,
                getClass().getSimpleName(),
                connectorName);
        return successfulOffsets;
    }

    @Override
    public void open(Collection<TopicPartition> partitions) {
        log.debug(
                "Opening task {}[{}] with partitions {}",
                getClass().getSimpleName(),
                connectorName,
                partitions);
    }

    @Override
    public void close(Collection<TopicPartition> partitions) {
        log.debug(
                "Closing task {}[{}] with partitions {}",
                getClass().getSimpleName(),
                connectorName,
                partitions);
        // clean offset cache
        successfulOffsets.clear();
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
                connectorName,
                sinkRecord.topic(),
                sinkRecord.kafkaPartition(),
                sinkRecord.kafkaOffset());
    }

    private void report(SinkRecord sinkRecord, Exception e) {
        if (!isDLQEnabled()) return;

        log.warn(
                "Reporting error [topic={}, partition={}, offset={}] for task {}[{}]",
                sinkRecord.topic(),
                sinkRecord.kafkaPartition(),
                sinkRecord.kafkaOffset(),
                getClass().getSimpleName(),
                connectorName);

        context.errantRecordReporter().report(sinkRecord, e);
    }

    private boolean doesTolerateErrors() {
        return errorsTolerance.equals("all");
    }

    private boolean isDLQEnabled() {
        try {
            return context.errantRecordReporter() != null;
        } catch (NoSuchMethodError | NoClassDefFoundError e) {
            return false;
        }
    }
}
