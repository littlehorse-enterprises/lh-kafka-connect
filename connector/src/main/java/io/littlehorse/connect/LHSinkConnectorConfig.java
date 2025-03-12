package io.littlehorse.connect;

import com.google.common.base.Strings;
import io.littlehorse.sdk.common.config.LHConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.apache.kafka.common.config.ConfigException;

@Getter
public class LHSinkConnectorConfig extends AbstractConfig {

    public static final String WF_SPEC_NAME_KEY = "wf.spec.name";
    public static final String CONNECTOR_NAME_KEY = "name";
    public static final String ERRORS_TOLERANCE_KEY = "errors.tolerance";
    public static final String LHC_API_HOST_KEY = parseKafkaConnectConfig(
        LHConfig.API_HOST_KEY
    );
    public static final String LHC_API_PORT_KEY = parseKafkaConnectConfig(
        LHConfig.API_PORT_KEY
    );
    public static final String LHC_TENANT_ID_KEY = parseKafkaConnectConfig(
        LHConfig.TENANT_ID_KEY
    );
    public static final String LHC_API_PROTOCOL_KEY = parseKafkaConnectConfig(
        LHConfig.API_PROTOCOL_KEY
    );
    public static final String LHC_OAUTH_CLIENT_ID_KEY =
        parseKafkaConnectConfig(LHConfig.OAUTH_CLIENT_ID_KEY);
    public static final String LHC_OAUTH_CLIENT_SECRET_KEY =
        parseKafkaConnectConfig(LHConfig.OAUTH_CLIENT_SECRET_KEY);
    public static final String LHC_OAUTH_ACCESS_TOKEN_URL_KEY =
        parseKafkaConnectConfig(LHConfig.OAUTH_ACCESS_TOKEN_URL_KEY);
    public static final String LHC_CLIENT_CERT_KEY = parseKafkaConnectConfig(
        LHConfig.CLIENT_CERT_KEY
    );
    public static final String LHC_CLIENT_KEY_KEY = parseKafkaConnectConfig(
        LHConfig.CLIENT_KEY_KEY
    );
    public static final String LHC_CA_CERT_KEY = parseKafkaConnectConfig(
        LHConfig.CA_CERT_KEY
    );
    public static final String LHC_GRPC_KEEPALIVE_TIME_MS_KEY =
        parseKafkaConnectConfig(LHConfig.GRPC_KEEPALIVE_TIME_MS_KEY);
    public static final String LHC_GRPC_KEEPALIVE_TIMEOUT_MS_KEY =
        parseKafkaConnectConfig(LHConfig.GRPC_KEEPALIVE_TIMEOUT_MS_KEY);

    public static final String PLAINTEXT = "PLAINTEXT";
    public static final String TLS = "TLS";

    public static final ConfigDef CONFIG_DEF = new ConfigDef()
        .define(
            LHC_API_HOST_KEY,
            Type.STRING,
            Importance.HIGH,
            "The bootstrap host for the LittleHorse Server."
        )
        .define(
            LHC_API_PORT_KEY,
            Type.INT,
            Importance.HIGH,
            "The bootstrap port for the LittleHorse Server."
        )
        .define(
            LHC_TENANT_ID_KEY,
            Type.STRING,
            "default",
            Importance.MEDIUM,
            "Tenant ID which represents a logically isolated environment within LittleHorse."
        )
        .define(
            LHC_API_PROTOCOL_KEY,
            Type.STRING,
            PLAINTEXT,
            ConfigDef.ValidString.in(PLAINTEXT, TLS),
            Importance.HIGH,
            "The bootstrap protocol for the LittleHorse Server."
        )
        .define(
            LHC_OAUTH_CLIENT_ID_KEY,
            Type.STRING,
            null,
            Importance.LOW,
            "Optional OAuth2 Client Id. Used by the Worker to identify itself at an Authorization Server. Client Credentials Flow."
        )
        .define(
            LHC_OAUTH_CLIENT_SECRET_KEY,
            Type.PASSWORD,
            null,
            Importance.LOW,
            "Optional OAuth2 Client Secret. Used by the Worker to identify itself at an Authorization Server. Client Credentials Flow."
        )
        .define(
            LHC_OAUTH_ACCESS_TOKEN_URL_KEY,
            Type.STRING,
            null,
            Importance.LOW,
            "Optional Access Token URL provided by the OAuth Authorization Server. Used by the Worker to obtain a token using client credentials flow."
        )
        .define(
            LHC_CLIENT_CERT_KEY,
            Type.STRING,
            null,
            Importance.LOW,
            "Optional location of Client Cert file for mTLS connection."
        )
        .define(
            LHC_CLIENT_KEY_KEY,
            Type.STRING,
            null,
            Importance.LOW,
            "Optional location of Client Private Key file for mTLS connection."
        )
        .define(
            LHC_CA_CERT_KEY,
            Type.STRING,
            null,
            Importance.LOW,
            "Optional location of CA Cert file that issued the server side certificates. For TLS and mTLS connection."
        )
        .define(
            LHC_GRPC_KEEPALIVE_TIME_MS_KEY,
            Type.LONG,
            Duration.ofSeconds(45).toMillis(),
            Importance.LOW,
            "Time in milliseconds to configure keepalive pings on the grpc client."
        )
        .define(
            LHC_GRPC_KEEPALIVE_TIMEOUT_MS_KEY,
            Type.LONG,
            Duration.ofSeconds(5).toMillis(),
            Importance.LOW,
            "Time in milliseconds to configure the timeout for the keepalive pings on the grpc client."
        )
        .define(
            WF_SPEC_NAME_KEY,
            Type.STRING,
            Importance.HIGH,
            "The name of the WfSpec to run."
        );

    private final String connectorName;
    private final String errorsTolerance;
    private final String wfSpecName;

    public LHSinkConnectorConfig(Map<?, ?> props) {
        super(CONFIG_DEF, props);
        connectorName = extractConnectorName(originalsStrings());
        errorsTolerance = extractErrorsTolerance(originalsStrings());
        wfSpecName = extractWfSpecName();
    }

    public String extractWfSpecName() {
        String wfSpecName = getString(WF_SPEC_NAME_KEY);
        if (Strings.isNullOrEmpty(wfSpecName)) {
            throw new ConfigException(WF_SPEC_NAME_KEY, wfSpecName);
        }
        return wfSpecName;
    }

    private static String extractErrorsTolerance(Map<String, String> props) {
        String errorsTolerance = props.get(ERRORS_TOLERANCE_KEY);
        return Strings.isNullOrEmpty(errorsTolerance)
            ? "none"
            : errorsTolerance;
    }

    private static String extractConnectorName(Map<String, String> props) {
        String name = props.get(CONNECTOR_NAME_KEY);
        if (Strings.isNullOrEmpty(name)) {
            throw new ConfigException(CONNECTOR_NAME_KEY, name);
        }
        return name;
    }

    private static String parseKafkaConnectConfig(String key) {
        // transform LH config (ex: LHC_API_HOST) into a kafka connect config (ex: lhc.api.host)
        return key.replace("_", ".").toLowerCase();
    }

    private static String parseLHConfig(String key) {
        // transform kafka connect config (ex: lhc.api.host) into LH config (ex: LHC_API_HOST)
        return key.replace(".", "_").toUpperCase();
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Path was expected but not provided");
            System.exit(1);
        }
        String docBuilder =
            "# lh-kafka-connect" +
            "\n\n" +
            "LittleHorse Sink Connector for Kafka Connect" +
            "\n\n" +
            "## Configurations" +
            "\n\n" +
            CONFIG_DEF.toEnrichedRst().strip() +
            "\n";
        Files.writeString(Path.of(args[0]), docBuilder);
    }

    public LHConfig toLHConfig() {
        return new LHConfig(toLHConfigMap());
    }

    public Map<String, Object> toLHConfigMap() {
        return nonInternalValues()
            .keySet()
            .stream()
            .filter(key -> LHConfig.configNames().contains(parseLHConfig(key)))
            .filter(key -> get(key) != null)
            .collect(
                Collectors.toMap(
                    LHSinkConnectorConfig::parseLHConfig,
                    this::get
                )
            );
    }
}
