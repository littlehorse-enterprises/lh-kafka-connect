package io.littlehorse.kafka.connect;

import com.google.protobuf.Empty;
import io.littlehorse.sdk.common.LHLibUtil;
import io.littlehorse.sdk.common.config.LHConfig;
import io.littlehorse.sdk.common.proto.LittleHorseGrpc.LittleHorseBlockingStub;
import io.littlehorse.sdk.common.proto.RunWfRequest;
import io.littlehorse.sdk.common.proto.ServerVersion;
import io.littlehorse.sdk.common.proto.VariableValue;
import java.util.Map;
import java.util.stream.Collectors;

public class LHSinkClient implements AutoCloseable {

    private final LittleHorseBlockingStub blockingStub;

    public LHSinkClient(LHConfig lhConfig) {
        this.blockingStub = lhConfig.getBlockingStub();
    }

    public String getServerVersion() {
        final ServerVersion response = blockingStub.getServerVersion(
            Empty.getDefaultInstance()
        );
        return String.format(
            "%s.%s.%s%s",
            response.getMajorVersion(),
            response.getMinorVersion(),
            response.getPatchVersion(),
            response.hasPreReleaseIdentifier()
                ? "-" + response.getPreReleaseIdentifier()
                : ""
        );
    }

    public void runWf(String id, Map<String, Object> variables) {
        Map<String, VariableValue> wfVariables = variables
            .keySet()
            .stream()
            .collect(
                Collectors.toMap(
                    key -> key,
                    key -> LHLibUtil.objToVarVal(variables.get(key))
                )
            );
        RunWfRequest request = RunWfRequest
            .newBuilder()
            .setWfSpecName("demo")
            .setId(id)
            .putAllVariables(wfVariables)
            .build();
        blockingStub.runWf(request);
    }

    @Override
    public void close() {}
}
