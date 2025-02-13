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
    public static final String LHC_TENANT_ID = "lhc.tenant.id";
    public static final String LHC_API_PORT = "lhc.api.port";
    public static final String LHC_API_PROTOCOL = "lhc.api.protocol";
    public static final String WF_SPEC_NAME = "wf.spec.name";
    public static final String LHC_OAUTH_CLIENT_ID = "lhc.oauth.client.id";
    public static final String LHC_OAUTH_CLIENT_SECRET =
        "lhc.oauth.client.secret";
    public static final String LHC_OAUTH_ACCESS_TOKEN_URL =
        "lhc.oauth.access.token.url";
    public static final String NAME = "name";

    public static final String EXPECTED_HOST = "localhost";
    public static final String EXPECTED_PORT = "2023";
    public static final String EXPECTED_TENANT = "my-tenant";
    public static final String EXPECTED_WF_SPEC_NAME = "my-wf";
    public static final String EXPECTED_NAME = "my-connector";

    @Test
    void shouldGenerateALHConfigObjectWithDefaultTenant() {
        LHSinkConnectorConfig connectorConfig = new LHSinkConnectorConfig(
            Map.of(
                NAME,
                EXPECTED_NAME,
                LHC_API_HOST,
                EXPECTED_HOST,
                LHC_API_PORT,
                EXPECTED_PORT,
                WF_SPEC_NAME,
                EXPECTED_WF_SPEC_NAME
            )
        );
        LHConfig lhConfig = connectorConfig.toLHConfig();
        assertThat(lhConfig.getApiBootstrapHost()).isEqualTo(EXPECTED_HOST);
        assertThat(lhConfig.getApiBootstrapPort())
            .isEqualTo(Integer.valueOf(EXPECTED_PORT));
        assertThat(lhConfig.getTenantId().getId()).isEqualTo("default");
    }

    @Test
    void shouldGenerateALHConfigObjectWithSpecificTenant() {
        LHSinkConnectorConfig connectorConfig = new LHSinkConnectorConfig(
            Map.of(
                NAME,
                EXPECTED_NAME,
                LHC_API_HOST,
                EXPECTED_HOST,
                LHC_API_PORT,
                EXPECTED_PORT,
                WF_SPEC_NAME,
                EXPECTED_WF_SPEC_NAME,
                LHC_TENANT_ID,
                EXPECTED_TENANT
            )
        );
        LHConfig lhConfig = connectorConfig.toLHConfig();
        assertThat(lhConfig.getTenantId().getId()).isEqualTo(EXPECTED_TENANT);
    }

    @Test
    void shouldIncludeOAuthConfigurationInLHConfig() {
        LHSinkConnectorConfig connectorConfig = new LHSinkConnectorConfig(
            Map.of(
                NAME,
                EXPECTED_NAME,
                LHC_API_HOST,
                EXPECTED_HOST,
                LHC_API_PORT,
                EXPECTED_PORT,
                WF_SPEC_NAME,
                EXPECTED_WF_SPEC_NAME,
                LHC_API_PROTOCOL,
                "TLS",
                LHC_OAUTH_CLIENT_ID,
                "my-client",
                LHC_OAUTH_CLIENT_SECRET,
                "my-secret",
                LHC_OAUTH_ACCESS_TOKEN_URL,
                "https://my-url.com/my-realm/token"
            )
        );
        LHConfig lhConfig = connectorConfig.toLHConfig();
        assertThat(lhConfig.isOauth()).isTrue();
    }

    @Test
    void shouldValidateAllowedLHProtocols() {
        assertThrows(
            ConfigException.class,
            () ->
                new LHSinkConnectorConfig(
                    Map.of(
                        LHC_API_HOST,
                        EXPECTED_HOST,
                        LHC_API_PORT,
                        EXPECTED_PORT,
                        LHC_API_PROTOCOL,
                        "NOT_A_PROTOCOL"
                    )
                )
        );
        assertDoesNotThrow(() ->
            new LHSinkConnectorConfig(
                Map.of(
                    NAME,
                    EXPECTED_NAME,
                    LHC_API_HOST,
                    EXPECTED_HOST,
                    LHC_API_PORT,
                    EXPECTED_PORT,
                    WF_SPEC_NAME,
                    EXPECTED_WF_SPEC_NAME,
                    LHC_API_PROTOCOL,
                    "TLS"
                )
            )
        );
        assertDoesNotThrow(() ->
            new LHSinkConnectorConfig(
                Map.of(
                    NAME,
                    EXPECTED_NAME,
                    LHC_API_HOST,
                    EXPECTED_HOST,
                    LHC_API_PORT,
                    EXPECTED_PORT,
                    WF_SPEC_NAME,
                    EXPECTED_WF_SPEC_NAME,
                    LHC_API_PROTOCOL,
                    "PLAINTEXT"
                )
            )
        );
    }

    @Test
    void shouldValidateMandatoryConfigs() {
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
        assertThrows(
            ConfigException.class,
            () ->
                new LHSinkConnectorConfig(
                    Map.of(WF_SPEC_NAME, EXPECTED_WF_SPEC_NAME)
                )
        );
        assertDoesNotThrow(() ->
            new LHSinkConnectorConfig(
                Map.of(
                    NAME,
                    EXPECTED_NAME,
                    LHC_API_HOST,
                    EXPECTED_HOST,
                    LHC_API_PORT,
                    EXPECTED_PORT,
                    WF_SPEC_NAME,
                    EXPECTED_WF_SPEC_NAME
                )
            )
        );
    }
}
