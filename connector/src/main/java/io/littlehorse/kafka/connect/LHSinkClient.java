package io.littlehorse.kafka.connect;

import com.google.protobuf.Empty;
import io.littlehorse.sdk.common.config.LHConfig;
import io.littlehorse.sdk.common.proto.LittleHorseGrpc.LittleHorseBlockingStub;
import io.littlehorse.sdk.common.proto.ServerVersion;

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

    @Override
    public void close() {}
}
