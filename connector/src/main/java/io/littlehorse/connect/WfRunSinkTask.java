package io.littlehorse.connect;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.littlehorse.sdk.common.LHLibUtil;
import io.littlehorse.sdk.common.proto.RunWfRequest;
import io.littlehorse.sdk.common.proto.VariableValue;
import io.littlehorse.sdk.common.proto.WfRunId;

import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.DataException;

import java.util.Map;
import java.util.function.Function;
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

    private Object removeInternalStruct(Object value) {
        if (value instanceof Struct) {
            Struct objectStruct = (Struct) value;
            Schema schema = objectStruct.schema();
            return schema.fields().stream()
                    .collect(Collectors.toMap(
                            Field::name, field -> removeInternalStruct(objectStruct.get(field))));
        }

        return value;
    }

    @SuppressWarnings("unchecked")
    private Map<String, VariableValue> extractVariable(Object value) {
        if (!(value instanceof Map) && !(value instanceof Struct)) {
            throw new DataException(
                    "Expected schema structure not provided, it should be a key-value pair data set");
        }

        Map<String, Object> variables = (Map<String, Object>) removeInternalStruct(value);

        return variables.keySet().stream()
                .collect(Collectors.toMap(
                        Function.identity(), key -> LHLibUtil.objToVarVal(variables.get(key))));
    }
}
