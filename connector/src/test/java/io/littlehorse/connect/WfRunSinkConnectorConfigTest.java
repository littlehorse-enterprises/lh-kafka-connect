package io.littlehorse.connect;

import static io.littlehorse.connect.LHSinkConnectorConfig.LHC_API_HOST_KEY;
import static io.littlehorse.connect.LHSinkConnectorConfig.LHC_API_PORT_KEY;
import static io.littlehorse.connect.WfRunSinkConnectorConfig.AUTO_IDEMPOTENCY_KEY_ENABLED_KEY;
import static io.littlehorse.connect.WfRunSinkConnectorConfig.WF_SPEC_NAME_KEY;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.littlehorse.sdk.common.config.LHConfig;

import org.apache.kafka.common.config.ConfigException;
import org.junit.jupiter.api.Test;

import java.util.Map;

class WfRunSinkConnectorConfigTest {
    @Test
    void shouldValidateWfSpecName() {
        assertThrows(
                ConfigException.class,
                () -> new WfRunSinkConnectorConfig(
                        Map.of(LHC_API_HOST_KEY, "localhost", LHC_API_PORT_KEY, 2023)));
    }

    @Test
    void shouldCreateNewConfig() {
        WfRunSinkConnectorConfig connectorConfig = new WfRunSinkConnectorConfig(Map.of(
                LHC_API_HOST_KEY,
                "localhost",
                LHC_API_PORT_KEY,
                2023,
                WF_SPEC_NAME_KEY,
                "my-workflow"));
        LHConfig lhConfig = connectorConfig.toLHConfig();
        assertThat(lhConfig.getApiBootstrapHost()).isEqualTo("localhost");
        assertThat(lhConfig.getApiBootstrapPort()).isEqualTo(Integer.valueOf(2023));
        assertThat(lhConfig.getTenantId().getId()).isEqualTo("default");
        assertThat(connectorConfig.getWfSpecName()).isEqualTo("my-workflow");
    }

    @Test
    void shouldEnableAutoIdempotencyKeyByDefault() {
        WfRunSinkConnectorConfig connectorConfig = new WfRunSinkConnectorConfig(Map.of(
                LHC_API_HOST_KEY,
                "localhost",
                LHC_API_PORT_KEY,
                2023,
                WF_SPEC_NAME_KEY,
                "my-workflow"));
        assertThat(connectorConfig.isAutoIdempotencyKeyEnabled()).isTrue();
    }

    @Test
    void shouldDisableAutoIdempotencyKey() {
        WfRunSinkConnectorConfig connectorConfig = new WfRunSinkConnectorConfig(Map.of(
                LHC_API_HOST_KEY,
                "localhost",
                LHC_API_PORT_KEY,
                2023,
                WF_SPEC_NAME_KEY,
                "my-workflow",
                AUTO_IDEMPOTENCY_KEY_ENABLED_KEY,
                false));
        assertThat(connectorConfig.isAutoIdempotencyKeyEnabled()).isFalse();
    }
}
