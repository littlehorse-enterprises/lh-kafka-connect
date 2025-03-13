package io.littlehorse.connect;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.littlehorse.sdk.common.proto.ExternalEventDefId;
import io.littlehorse.sdk.common.proto.PutExternalEventRequest;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class ExternalEventSinkTask extends LHSinkTask {

    private ExternalEventSinkConnectorConfig config;

    @Override
    public LHSinkConnectorConfig initializeConfig(Map<String, String> props) {
        return config = new ExternalEventSinkConnectorConfig(props);
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
        PutExternalEventRequest.Builder requestBuilder = PutExternalEventRequest.newBuilder()
                .setGuid(sinkRecord.getIdempotencyKey())
                .setExternalEventDefId(
                        ExternalEventDefId.newBuilder().setName(config.getExternalEventName()));

        if (config.getThreadRunNumber() != null) {
            requestBuilder.setThreadRunNumber(config.getThreadRunNumber());
        }

        if (config.getNodeRunPosition() != null) {
            requestBuilder.setNodeRunPosition(config.getNodeRunPosition());
        }

        // set content
        // set wf run id

        return requestBuilder.build();
    }
}
