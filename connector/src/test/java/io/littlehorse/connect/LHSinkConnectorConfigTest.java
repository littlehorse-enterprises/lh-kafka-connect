package io.littlehorse.connect;

import static io.littlehorse.connect.LHSinkConnectorConfig.BASE_CONFIG_DEF;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.littlehorse.sdk.common.config.LHConfig;

import org.apache.kafka.common.config.ConfigException;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class LHSinkConnectorConfigTest {

    public static final String LHC_API_HOST = "lhc.api.host";
    public static final String LHC_TENANT_ID = "lhc.tenant.id";
    public static final String LHC_API_PORT = "lhc.api.port";
    public static final String LHC_API_PROTOCOL = "lhc.api.protocol";
    public static final String LHC_OAUTH_CLIENT_ID = "lhc.oauth.client.id";
    public static final String LHC_OAUTH_CLIENT_SECRET = "lhc.oauth.client.secret";
    public static final String LHC_OAUTH_ACCESS_TOKEN_URL = "lhc.oauth.access.token.url";
    public static final String NAME = "name";

    public static final String EXPECTED_HOST = "localhost";
    public static final String EXPECTED_PORT = "2023";
    public static final String EXPECTED_TENANT = "my-tenant";
    public static final String EXPECTED_NAME = "my-connector";

    @Test
    void shouldGenerateALHConfigObjectWithDefaultTenant() {
        LHSinkConnectorConfig connectorConfig = new LHSinkConnectorConfig(
                BASE_CONFIG_DEF,
                Map.of(
                        NAME,
                        EXPECTED_NAME,
                        LHC_API_HOST,
                        EXPECTED_HOST,
                        LHC_API_PORT,
                        EXPECTED_PORT)) {};
        LHConfig lhConfig = connectorConfig.toLHConfig();
        assertThat(lhConfig.getApiBootstrapHost()).isEqualTo(EXPECTED_HOST);
        assertThat(lhConfig.getApiBootstrapPort()).isEqualTo(Integer.valueOf(EXPECTED_PORT));
        assertThat(lhConfig.getTenantId().getId()).isEqualTo("default");
    }

    @Test
    void shouldGenerateALHConfigObjectWithSpecificTenant() {
        LHSinkConnectorConfig connectorConfig = new LHSinkConnectorConfig(
                BASE_CONFIG_DEF,
                Map.of(
                        NAME,
                        EXPECTED_NAME,
                        LHC_API_HOST,
                        EXPECTED_HOST,
                        LHC_API_PORT,
                        EXPECTED_PORT,
                        LHC_TENANT_ID,
                        EXPECTED_TENANT)) {};
        LHConfig lhConfig = connectorConfig.toLHConfig();
        assertThat(lhConfig.getTenantId().getId()).isEqualTo(EXPECTED_TENANT);
    }

    @Test
    void shouldIncludeOAuthConfigurationInLHConfig() {
        LHSinkConnectorConfig connectorConfig = new LHSinkConnectorConfig(
                BASE_CONFIG_DEF,
                Map.of(
                        NAME,
                        EXPECTED_NAME,
                        LHC_API_HOST,
                        EXPECTED_HOST,
                        LHC_API_PORT,
                        EXPECTED_PORT,
                        LHC_API_PROTOCOL,
                        "TLS",
                        LHC_OAUTH_CLIENT_ID,
                        "my-client",
                        LHC_OAUTH_CLIENT_SECRET,
                        "my-secret",
                        LHC_OAUTH_ACCESS_TOKEN_URL,
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
                                LHC_API_HOST,
                                EXPECTED_HOST,
                                LHC_API_PORT,
                                EXPECTED_PORT,
                                LHC_API_PROTOCOL,
                                "NOT_A_PROTOCOL")) {});
        assertDoesNotThrow(() -> new LHSinkConnectorConfig(
                BASE_CONFIG_DEF,
                Map.of(
                        NAME,
                        EXPECTED_NAME,
                        LHC_API_HOST,
                        EXPECTED_HOST,
                        LHC_API_PORT,
                        EXPECTED_PORT,
                        LHC_API_PROTOCOL,
                        "TLS")) {});
        assertDoesNotThrow(() -> new LHSinkConnectorConfig(
                BASE_CONFIG_DEF,
                Map.of(
                        NAME,
                        EXPECTED_NAME,
                        LHC_API_HOST,
                        EXPECTED_HOST,
                        LHC_API_PORT,
                        EXPECTED_PORT,
                        LHC_API_PROTOCOL,
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
                        BASE_CONFIG_DEF, Map.of(LHC_API_HOST, EXPECTED_HOST)) {});
        assertThrows(
                ConfigException.class,
                () -> new LHSinkConnectorConfig(
                        BASE_CONFIG_DEF, Map.of(LHC_API_PORT, EXPECTED_PORT)) {});
        assertDoesNotThrow(() -> new LHSinkConnectorConfig(
                BASE_CONFIG_DEF,
                Map.of(
                        NAME,
                        EXPECTED_NAME,
                        LHC_API_HOST,
                        EXPECTED_HOST,
                        LHC_API_PORT,
                        EXPECTED_PORT)) {});
    }
}
