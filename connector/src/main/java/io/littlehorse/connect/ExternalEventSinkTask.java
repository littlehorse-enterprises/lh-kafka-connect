package io.littlehorse.connect;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.littlehorse.connect.record.IdempotentSinkRecord;
import io.littlehorse.connect.util.ObjectMapper;
import io.littlehorse.connect.util.VariableValueMapper;
import io.littlehorse.sdk.common.LHLibUtil;
import io.littlehorse.sdk.common.proto.ExternalEventDef;
import io.littlehorse.sdk.common.proto.ExternalEventDefId;
import io.littlehorse.sdk.common.proto.PutExternalEventRequest;
import io.littlehorse.sdk.common.proto.TypeDefinition;

import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.connect.errors.DataException;

import java.util.Map;

@Slf4j
public class ExternalEventSinkTask extends LHSinkTask {

    private ExternalEventSinkConnectorConfig config;
    private VariableValueMapper variableMapper;
    private TypeDefinition contentType;
    private ObjectMapper objectMapper;

    @Override
    public LHSinkConnectorConfig configure(Map<String, String> props) {
        return config = new ExternalEventSinkConnectorConfig(props);
    }

    @Override
    protected void afterStart() {
        variableMapper = new VariableValueMapper(getBlockingStub());
        objectMapper = new ObjectMapper();
        loadContentType();
    }

    private void loadContentType() {
        ExternalEventDef externalEventDef = getBlockingStub()
                .getExternalEventDef(ExternalEventDefId.newBuilder()
                        .setName(config.getExternalEventName())
                        .build());
        if (externalEventDef.hasTypeInformation()) {
            contentType = externalEventDef.getTypeInformation().getReturnType();
        }
    }

    @Override
    public void executeGrpcCall(IdempotentSinkRecord sinkRecord) {
        try {
            // blocking the thread we ensure sequential order by partition
            getBlockingStub().putExternalEvent(buildRequest(sinkRecord));
        } catch (StatusRuntimeException grpcException) {
            if (grpcException.getStatus().getCode() != Status.ALREADY_EXISTS.getCode()) {
                throw grpcException;
            }
            log.warn("ExternalEvent already exists, skipping");
        }
    }

    private PutExternalEventRequest buildRequest(IdempotentSinkRecord sinkRecord) {
        return PutExternalEventRequest.newBuilder()
                .setGuid(
                        sinkRecord.guid() == null ? sinkRecord.idempotencyKey() : sinkRecord.guid())
                .setWfRunId(LHLibUtil.wfRunIdFromString(
                        sinkRecord.wfRunId() == null
                                ? extractWfRunId(sinkRecord.key())
                                : sinkRecord.wfRunId()))
                .setContent(variableMapper.toVariableValue(
                        objectMapper.removeStruct(sinkRecord.value()), contentType))
                .setExternalEventDefId(
                        ExternalEventDefId.newBuilder().setName(config.getExternalEventName()))
                .build();
    }

    private String extractWfRunId(Object object) {
        if (!(object instanceof String)) {
            throw new DataException(
                    "Expected schema structure not provided, key should be a String object");
        }
        return (String) object;
    }
}
