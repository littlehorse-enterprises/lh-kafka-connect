package io.littlehorse.connect;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.littlehorse.connect.record.IdempotentSinkRecord;
import io.littlehorse.connect.util.ObjectMapper;
import io.littlehorse.sdk.common.LHLibUtil;
import io.littlehorse.sdk.common.proto.RunWfRequest;
import io.littlehorse.sdk.common.proto.VariableValue;
import io.littlehorse.sdk.common.proto.WfRunId;

import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.DataException;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class WfRunSinkTask extends LHSinkTask {

    private WfRunSinkConnectorConfig config;

    @Override
    public LHSinkConnectorConfig initializeConfig(Map<String, String> props) {
        return config = new WfRunSinkConnectorConfig(props);
    }

    @Override
    public void executeGrpcCall(IdempotentSinkRecord sinkRecord) {
        try {
            // blocking the thread we ensure sequential order by partition
            getBlockingStub().runWf(buildRequest(sinkRecord));
        } catch (StatusRuntimeException grpcException) {
            if (grpcException.getStatus().getCode() != Status.ALREADY_EXISTS.getCode()) {
                throw grpcException;
            }
            log.warn("WfRun already exists, skipping");
        }
    }

    private RunWfRequest buildRequest(IdempotentSinkRecord sinkRecord) {
        RunWfRequest.Builder requestBuilder = RunWfRequest.newBuilder()
                .setWfSpecName(config.getWfSpecName())
                .setId(sinkRecord.getIdempotencyKey())
                .putAllVariables(extractVariable(sinkRecord.value()));

        if (config.getWfRunParentId() != null) {
            requestBuilder.setParentWfRunId(WfRunId.newBuilder().setId(config.getWfRunParentId()));
        }

        if (config.getWfSpecMajorVersion() != null) {
            requestBuilder.setMajorVersion(config.getWfSpecMajorVersion());
        }

        if (config.getWfSpecRevision() != null) {
            requestBuilder.setRevision(config.getWfSpecRevision());
        }

        return requestBuilder.build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, VariableValue> extractVariable(Object value) {
        if (!(value instanceof Map) && !(value instanceof Struct)) {
            throw new DataException(
                    "Expected schema structure not provided, it should be a key-value pair data set");
        }

        Map<String, Object> variables = (Map<String, Object>) ObjectMapper.removeStruct(value);
        return variables.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey, entry -> LHLibUtil.objToVarVal(entry.getValue())));
    }
}
