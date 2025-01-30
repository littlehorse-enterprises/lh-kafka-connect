package io.littlehorse.kafka.connect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.littlehorse.sdk.common.config.LHConfig;
import java.util.Map;
import org.apache.kafka.common.config.ConfigException;
import org.junit.jupiter.api.Test;

public class LHSinkConnectorConfigTest {

    public static final String LHC_API_HOST = "lhc.api.host";
    public static final String LHC_API_PORT = "lhc.api.port";
    public static final String EXPECTED_HOST = "localhost";
    public static final int EXPECTED_PORT = 2023;
    public static final String EXPECTED_TENANT = "default";

    @Test
    void toLHConfig() {
        LHSinkConnectorConfig connectorConfig = new LHSinkConnectorConfig(
            Map.of(LHC_API_HOST, EXPECTED_HOST, LHC_API_PORT, EXPECTED_PORT)
        );
        LHConfig lhConfig = connectorConfig.toLHConfig();
        assertThat(lhConfig.getApiBootstrapHost()).isEqualTo(EXPECTED_HOST);
        assertThat(lhConfig.getApiBootstrapPort()).isEqualTo(EXPECTED_PORT);
        assertThat(lhConfig.getTenantId().getId()).isEqualTo(EXPECTED_TENANT);
    }

    @Test
    void validateMandatory() {
        assertThrows(
            ConfigException.class,
            () -> new LHSinkConnectorConfig(Map.of())
        );
        assertThrows(
            ConfigException.class,
            () -> new LHSinkConnectorConfig(Map.of(LHC_API_HOST, EXPECTED_HOST))
        );
        assertThrows(
            ConfigException.class,
            () -> new LHSinkConnectorConfig(Map.of(LHC_API_PORT, EXPECTED_PORT))
        );
        assertDoesNotThrow(() ->
            new LHSinkConnectorConfig(
                Map.of(LHC_API_HOST, EXPECTED_HOST, LHC_API_PORT, EXPECTED_PORT)
            )
        );
    }
}
