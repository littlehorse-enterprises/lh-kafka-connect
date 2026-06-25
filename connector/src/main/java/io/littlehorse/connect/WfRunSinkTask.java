package io.littlehorse.connect;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.littlehorse.connect.record.IdempotentSinkRecord;
import io.littlehorse.connect.util.ObjectMapper;
import io.littlehorse.connect.util.VariableValueMapper;
import io.littlehorse.sdk.common.LHLibUtil;
import io.littlehorse.sdk.common.proto.GetLatestWfSpecRequest;
import io.littlehorse.sdk.common.proto.RunWfRequest;
import io.littlehorse.sdk.common.proto.ThreadSpec;
import io.littlehorse.sdk.common.proto.ThreadVarDef;
import io.littlehorse.sdk.common.proto.TypeDefinition;
import io.littlehorse.sdk.common.proto.VariableDef;
import io.littlehorse.sdk.common.proto.VariableValue;
import io.littlehorse.sdk.common.proto.WfSpec;
import io.littlehorse.sdk.common.proto.WfSpecId;

import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.DataException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class WfRunSinkTask extends LHSinkTask {

    private WfRunSinkConnectorConfig config;
    private final Map<String, TypeDefinition> variableTypeDefs = new HashMap<>();
    private ObjectMapper objectMapper;
    private VariableValueMapper variableMapper;

    @Override
    public LHSinkConnectorConfig configure(Map<String, String> props) {
        return config = new WfRunSinkConnectorConfig(props);
    }

    @Override
    protected void afterStart() {
        variableMapper = new VariableValueMapper(getBlockingStub());
        objectMapper = new ObjectMapper();
        loadVariableTypeDefs();
    }

    private void loadVariableTypeDefs() {
        WfSpec wfSpec = fetchWfSpec();
        ThreadSpec entrypoint = wfSpec.getThreadSpecsOrThrow(wfSpec.getEntrypointThreadName());

        for (ThreadVarDef threadVarDef : entrypoint.getVariableDefsList()) {
            VariableDef varDef = threadVarDef.getVarDef();
            variableTypeDefs.put(varDef.getName(), varDef.getTypeDef());
        }
    }

    private WfSpec fetchWfSpec() {
        if (config.getWfSpecMajorVersion() != null && config.getWfSpecRevision() != null) {
            WfSpecId wfSpecId = WfSpecId.newBuilder()
                    .setName(config.getWfSpecName())
                    .setMajorVersion(config.getWfSpecMajorVersion())
                    .setRevision(config.getWfSpecRevision())
                    .build();
            return getBlockingStub().getWfSpec(wfSpecId);
        }

        GetLatestWfSpecRequest.Builder request =
                GetLatestWfSpecRequest.newBuilder().setName(config.getWfSpecName());

        if (config.getWfSpecMajorVersion() != null) {
            request.setMajorVersion(config.getWfSpecMajorVersion());
        }

        return getBlockingStub().getLatestWfSpec(request.build());
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
                .setId(extractWfRunId(sinkRecord));

        Object value = sinkRecord.value();
        if (value != null) {
            requestBuilder.putAllVariables(extractVariables(value));
        }

        String parentWfRunId = sinkRecord.parentWfRunId() == null
                ? config.getWfRunParentId()
                : sinkRecord.parentWfRunId();

        if (parentWfRunId != null) {
            requestBuilder.setParentWfRunId(LHLibUtil.wfRunIdFromString(parentWfRunId));
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
    private Map<String, VariableValue> extractVariables(Object value) {
        if (!(value instanceof Map) && !(value instanceof Struct)) {
            throw new DataException(
                    "Expected schema structure not provided, it should be a key-value pair data set");
        }

        Map<String, Object> variables = (Map<String, Object>) objectMapper.removeStruct(value);
        Map<String, VariableValue> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            result.put(entry.getKey(), buildVariableValue(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    private VariableValue buildVariableValue(String name, Object value) {
        return variableMapper.toVariableValue(value, variableTypeDefs.get(name));
    }

    private String extractWfRunId(IdempotentSinkRecord sinkRecord) {
        if (sinkRecord.wfRunId() != null) {
            return sinkRecord.wfRunId();
        }

        if (sinkRecord.key() != null) {
            if (!(sinkRecord.key() instanceof String)) {
                throw new DataException(
                        "Expected schema structure not provided, key should be a String object");
            }

            return sinkRecord.key().toString();
        }

        return sinkRecord.idempotencyKey();
    }
}
