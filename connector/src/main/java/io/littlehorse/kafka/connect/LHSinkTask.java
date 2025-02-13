package io.littlehorse.kafka.connect;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.littlehorse.sdk.common.LHLibUtil;
import io.littlehorse.sdk.common.config.LHConfig;
import io.littlehorse.sdk.common.proto.LittleHorseGrpc.LittleHorseBlockingStub;
import io.littlehorse.sdk.common.proto.RunWfRequest;
import io.littlehorse.sdk.common.proto.VariableValue;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;

@Slf4j
public class LHSinkTask extends SinkTask {

    private final Map<TopicPartition, OffsetAndMetadata> successfulOffsets =
        new HashMap<>();
    private LHSinkConnectorConfig connectorConfig;
    private LittleHorseBlockingStub blockingStub;
    private LHConfig lhConfig;

    @Override
    public String version() {
        return LHSinkConnectorVersion.version();
    }

    @Override
    public void start(Map<String, String> props) {
        connectorConfig = new LHSinkConnectorConfig(props);
        lhConfig = connectorConfig.toLHConfig();
        blockingStub = lhConfig.getBlockingStub();
        log.debug(
            "Starting tasks {} with DLQ {}",
            connectorConfig.getConnectorName(),
            isDLQEnabled() ? "enabled" : "disabled"
        );
    }

    @Override
    public void stop() {
        log.debug("Closing LHSinkTask");
        // TODO: close channels
        // lhConfig.closeConnections();
    }

    @Override
    public void put(Collection<SinkRecord> records) {
        records.forEach(sinkRecord -> {
            log.debug(
                "Processing record [topic={}, partition={}, offset={}]",
                sinkRecord.topic(),
                sinkRecord.kafkaPartition(),
                sinkRecord.kafkaOffset()
            );

            try {
                runWf(sinkRecord);
                updateSuccessfulOffsets(sinkRecord);
            } catch (Exception e) {
                log.error("Error processing record {}", e.getMessage(), e);

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
            new OffsetAndMetadata(sinkRecord.kafkaOffset() + 1)
        );
    }

    private String calculateWfRunId(SinkRecord sinkRecord) {
        // to ensure idempotency we use: connector name + topic + partition + offset
        return String.format(
            "%s-%s-%d-%d",
            connectorConfig.getConnectorName(),
            sinkRecord.topic(),
            sinkRecord.kafkaPartition(),
            sinkRecord.kafkaOffset()
        );
    }

    private void runWf(SinkRecord sinkRecord) {
        try {
            // it is supposed to be a json, it should be a Struct if using Schema Registry
            @SuppressWarnings("unchecked")
            Map<String, Object> value = (Map<
                    String,
                    Object
                >) sinkRecord.value();

            // extract all fields inside the json struct
            Map<String, VariableValue> variables = value
                .keySet()
                .stream()
                .collect(
                    Collectors.toMap(
                        Function.identity(),
                        field -> LHLibUtil.objToVarVal(value.get(field))
                    )
                );

            RunWfRequest request = RunWfRequest
                .newBuilder()
                .setWfSpecName(connectorConfig.getWfSpecName())
                .setId(calculateWfRunId(sinkRecord))
                .putAllVariables(variables)
                .build();

            // blocking the thread we ensure sequential order by partition
            blockingStub.runWf(request);
        } catch (StatusRuntimeException grpcException) {
            if (
                grpcException.getStatus().getCode() !=
                Status.ALREADY_EXISTS.getCode()
            ) {
                throw grpcException;
            }
            log.warn("WfRun already exists, skipping");
        }
    }

    private void report(SinkRecord sinkRecord, Exception e) {
        if (!isDLQEnabled()) return;

        log.warn(
            "Reporting error [topic={}, partition={}, offset={}]",
            sinkRecord.topic(),
            sinkRecord.kafkaPartition(),
            sinkRecord.kafkaOffset(),
            e
        );

        context.errantRecordReporter().report(sinkRecord, e);
    }

    private boolean doesTolerateErrors() {
        return connectorConfig.getErrorsTolerance().equals("all");
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
        Map<TopicPartition, OffsetAndMetadata> currentOffsets
    ) {
        log.debug("Commiting partitions {}", successfulOffsets);
        return successfulOffsets;
    }

    @Override
    public void open(Collection<TopicPartition> partitions) {
        log.debug("Opening task with partitions {}", partitions);
    }

    @Override
    public void close(Collection<TopicPartition> partitions) {
        log.debug("Closing task with partitions {}", partitions);
        // clean offset cache
        successfulOffsets.clear();
    }
}
