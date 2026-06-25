package io.littlehorse.connect;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.littlehorse.connect.record.IdempotentSinkRecord;
import io.littlehorse.connect.util.ObjectMapper;
import io.littlehorse.connect.util.StructValueMapper;
import io.littlehorse.sdk.common.proto.ExternalEventDef;
import io.littlehorse.sdk.common.proto.ExternalEventDefId;
import io.littlehorse.sdk.common.proto.PutCorrelatedEventRequest;
import io.littlehorse.sdk.common.proto.TypeDefinition;

import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.connect.errors.DataException;

import java.util.Map;

@Slf4j
public class CorrelatedEventSinkTask extends LHSinkTask {

    private CorrelatedEventSinkConnectorConfig config;
    private StructValueMapper structMapper;
    private TypeDefinition contentType;

    @Override
    public LHSinkConnectorConfig configure(Map<String, String> props) {
        return config = new CorrelatedEventSinkConnectorConfig(props);
    }

    @Override
    protected void afterStart() {
        structMapper = new StructValueMapper(getBlockingStub());
        loadContentType();
    }

    private void loadContentType() {
        ExternalEventDef externalEventDef = getBlockingStub()
                .getExternalEventDef(ExternalEventDefId.newBuilder()
                        .setName(config.getExternalEventName())
                        .build());
        contentType = externalEventDef.getTypeInformation().getReturnType();
    }

    @Override
    public void executeGrpcCall(IdempotentSinkRecord sinkRecord) {
        try {
            // blocking the thread we ensure sequential order by partition
            getBlockingStub().putCorrelatedEvent(buildRequest(sinkRecord));
        } catch (StatusRuntimeException grpcException) {
            if (grpcException.getStatus().getCode() != Status.ALREADY_EXISTS.getCode()) {
                throw grpcException;
            }
            log.warn("CorrelatedEvent already exists, skipping");
        }
    }

    private PutCorrelatedEventRequest buildRequest(IdempotentSinkRecord sinkRecord) {
        return PutCorrelatedEventRequest.newBuilder()
                .setKey(
                        sinkRecord.correlationId() == null
                                ? extractCorrelationId(sinkRecord.key())
                                : sinkRecord.correlationId())
                .setContent(structMapper.toVariableValue(
                        ObjectMapper.removeStruct(sinkRecord.value()), contentType))
                .setExternalEventDefId(
                        ExternalEventDefId.newBuilder().setName(config.getExternalEventName()))
                .build();
    }

    private String extractCorrelationId(Object object) {
        if (!(object instanceof String)) {
            throw new DataException(
                    "Expected schema structure not provided, key should be a String object");
        }
        return (String) object;
    }
}
