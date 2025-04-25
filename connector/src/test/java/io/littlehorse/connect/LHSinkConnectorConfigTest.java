package io.littlehorse.connect;

import static io.littlehorse.connect.LHSinkConnectorConfig.BASE_CONFIG_DEF;
import static io.littlehorse.connect.LHSinkConnectorConfig.LHC_API_HOST_KEY;
import static io.littlehorse.connect.LHSinkConnectorConfig.LHC_API_PORT_KEY;
import static io.littlehorse.connect.LHSinkConnectorConfig.LHC_API_PROTOCOL_KEY;
import static io.littlehorse.connect.LHSinkConnectorConfig.LHC_OAUTH_ACCESS_TOKEN_URL_KEY;
import static io.littlehorse.connect.LHSinkConnectorConfig.LHC_OAUTH_CLIENT_ID_KEY;
import static io.littlehorse.connect.LHSinkConnectorConfig.LHC_OAUTH_CLIENT_SECRET_KEY;
import static io.littlehorse.connect.LHSinkConnectorConfig.LHC_TENANT_ID_KEY;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.littlehorse.sdk.common.config.LHConfig;

import org.apache.kafka.common.config.ConfigException;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class LHSinkConnectorConfigTest {

    public static final String EXPECTED_HOST = "localhost";
    public static final String EXPECTED_PORT = "2023";
    public static final String EXPECTED_TENANT = "my-tenant";

    @Test
    void shouldGenerateLHConfigObjectWithDefaultTenant() {
        LHSinkConnectorConfig connectorConfig = new LHSinkConnectorConfig(
                BASE_CONFIG_DEF,
                Map.of(LHC_API_HOST_KEY, EXPECTED_HOST, LHC_API_PORT_KEY, EXPECTED_PORT)) {};
        LHConfig lhConfig = connectorConfig.toLHConfig();
        assertThat(lhConfig.getApiBootstrapHost()).isEqualTo(EXPECTED_HOST);
        assertThat(lhConfig.getApiBootstrapPort()).isEqualTo(Integer.valueOf(EXPECTED_PORT));
        assertThat(lhConfig.getTenantId().getId()).isEqualTo("default");
    }

    @Test
    void shouldGenerateLHConfigObjectWithSpecificTenant() {
        LHSinkConnectorConfig connectorConfig = new LHSinkConnectorConfig(
                BASE_CONFIG_DEF,
                Map.of(
                        LHC_API_HOST_KEY,
                        EXPECTED_HOST,
                        LHC_API_PORT_KEY,
                        EXPECTED_PORT,
                        LHC_TENANT_ID_KEY,
                        EXPECTED_TENANT)) {};
        LHConfig lhConfig = connectorConfig.toLHConfig();
        assertThat(lhConfig.getTenantId().getId()).isEqualTo(EXPECTED_TENANT);
    }

    @Test
    void shouldIncludeOAuthConfigurationInLHConfig() {
        LHSinkConnectorConfig connectorConfig = new LHSinkConnectorConfig(
                BASE_CONFIG_DEF,
                Map.of(
                        LHC_API_HOST_KEY,
                        EXPECTED_HOST,
                        LHC_API_PORT_KEY,
                        EXPECTED_PORT,
                        LHC_API_PROTOCOL_KEY,
                        "TLS",
                        LHC_OAUTH_CLIENT_ID_KEY,
                        "my-client",
                        LHC_OAUTH_CLIENT_SECRET_KEY,
                        "my-secret",
                        LHC_OAUTH_ACCESS_TOKEN_URL_KEY,
                        "https://my-url.com/my-realm/token")) {};
        LHConfig lhConfig = connectorConfig.toLHConfig();
        assertThat(lhConfig.isOauth()).isTrue();
    }

    @Test
    void shouldValidateAllowedLHProtocols() {
        assertThrows(
                ConfigException.class,
                () -> new LHSinkConnectorConfig(
                        BASE_CONFIG_DEF,
                        Map.of(
                                LHC_API_HOST_KEY,
                                EXPECTED_HOST,
                                LHC_API_PORT_KEY,
                                EXPECTED_PORT,
                                LHC_API_PROTOCOL_KEY,
                                "NOT_A_PROTOCOL")) {});
        assertDoesNotThrow(() -> new LHSinkConnectorConfig(
                BASE_CONFIG_DEF,
                Map.of(
                        LHC_API_HOST_KEY,
                        EXPECTED_HOST,
                        LHC_API_PORT_KEY,
                        EXPECTED_PORT,
                        LHC_API_PROTOCOL_KEY,
                        "TLS")) {});
        assertDoesNotThrow(() -> new LHSinkConnectorConfig(
                BASE_CONFIG_DEF,
                Map.of(
                        LHC_API_HOST_KEY,
                        EXPECTED_HOST,
                        LHC_API_PORT_KEY,
                        EXPECTED_PORT,
                        LHC_API_PROTOCOL_KEY,
                        "PLAINTEXT")) {});
    }

    @Test
    void shouldValidateMandatoryConfigs() {
        assertThrows(
                ConfigException.class,
                () -> new LHSinkConnectorConfig(BASE_CONFIG_DEF, Map.of()) {});
        assertThrows(
                ConfigException.class,
                () -> new LHSinkConnectorConfig(
                        BASE_CONFIG_DEF, Map.of(LHC_API_HOST_KEY, EXPECTED_HOST)) {});
        assertThrows(
                ConfigException.class,
                () -> new LHSinkConnectorConfig(
                        BASE_CONFIG_DEF, Map.of(LHC_API_PORT_KEY, EXPECTED_PORT)) {});
        assertDoesNotThrow(() -> new LHSinkConnectorConfig(
                BASE_CONFIG_DEF,
                Map.of(LHC_API_HOST_KEY, EXPECTED_HOST, LHC_API_PORT_KEY, EXPECTED_PORT)) {});
    }
}
