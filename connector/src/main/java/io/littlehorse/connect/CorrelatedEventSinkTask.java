package io.littlehorse.connect;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.littlehorse.connect.record.IdempotentSinkRecord;
import io.littlehorse.connect.util.ObjectMapper;
import io.littlehorse.sdk.common.LHLibUtil;
import io.littlehorse.sdk.common.proto.ExternalEventDefId;
import io.littlehorse.sdk.common.proto.PutCorrelatedEventRequest;

import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.connect.errors.DataException;

import java.util.Map;

@Slf4j
public class CorrelatedEventSinkTask extends LHSinkTask {

    private CorrelatedEventSinkConnectorConfig config;

    @Override
    public LHSinkConnectorConfig configure(Map<String, String> props) {
        return config = new CorrelatedEventSinkConnectorConfig(props);
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
                .setContent(LHLibUtil.objToVarVal(ObjectMapper.removeStruct(sinkRecord.value())))
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
