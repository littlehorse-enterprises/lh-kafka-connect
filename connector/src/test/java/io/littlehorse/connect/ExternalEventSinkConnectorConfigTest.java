package io.littlehorse.connect;

import static io.littlehorse.connect.ExternalEventSinkConnectorConfig.EXTERNAL_EVENT_NAME_KEY;
import static io.littlehorse.connect.LHSinkConnectorConfig.LHC_API_HOST_KEY;
import static io.littlehorse.connect.LHSinkConnectorConfig.LHC_API_PORT_KEY;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.littlehorse.sdk.common.config.LHConfig;

import org.apache.kafka.common.config.ConfigException;
import org.junit.jupiter.api.Test;

import java.util.Map;

class ExternalEventSinkConnectorConfigTest {
    @Test
    void shouldValidateExternalEvent() {
        assertThrows(
                ConfigException.class,
                () -> new ExternalEventSinkConnectorConfig(
                        Map.of(LHC_API_HOST_KEY, "localhost", LHC_API_PORT_KEY, 2023)));
    }

    @Test
    void shouldCreateNewConfig() {
        ExternalEventSinkConnectorConfig connectorConfig =
                new ExternalEventSinkConnectorConfig(Map.of(
                        LHC_API_HOST_KEY,
                        "localhost",
                        LHC_API_PORT_KEY,
                        2023,
                        EXTERNAL_EVENT_NAME_KEY,
                        "my-external-event"));
        LHConfig lhConfig = connectorConfig.toLHConfig();
        assertThat(lhConfig.getApiBootstrapHost()).isEqualTo("localhost");
        assertThat(lhConfig.getApiBootstrapPort()).isEqualTo(Integer.valueOf(2023));
        assertThat(lhConfig.getTenantId().getId()).isEqualTo("default");
        assertThat(connectorConfig.getExternalEventName()).isEqualTo("my-external-event");
    }
}
