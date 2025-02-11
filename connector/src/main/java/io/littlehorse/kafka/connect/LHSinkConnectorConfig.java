package io.littlehorse.kafka.connect;

import io.littlehorse.sdk.common.config.LHConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;

public class LHSinkConnectorConfig extends AbstractConfig {

    public static final ConfigDef CONFIG_DEF = new ConfigDef()
        .define(
            parseKafkaConnectConfig(LHConfig.API_HOST_KEY),
            Type.STRING,
            Importance.HIGH,
            "The bootstrap host for the LittleHorse Server."
        )
        .define(
            parseKafkaConnectConfig(LHConfig.API_PORT_KEY),
            Type.INT,
            Importance.HIGH,
            "The bootstrap port for the LittleHorse Server."
        )
        .define(
            parseKafkaConnectConfig(LHConfig.TENANT_ID_KEY),
            Type.STRING,
            "default",
            Importance.MEDIUM,
            "Tenant ID which represents a logically isolated environment within LittleHorse."
        )
        .define(
            parseKafkaConnectConfig(LHConfig.API_PROTOCOL_KEY),
            Type.STRING,
            "PLAINTEXT",
            ConfigDef.ValidString.in("PLAINTEXT", "TLS"),
            Importance.HIGH,
            "The bootstrap protocol for the LittleHorse Server. Valid values: PLAINTEXT and TLS."
        )
        .define(
            parseKafkaConnectConfig(LHConfig.OAUTH_CLIENT_ID_KEY),
            Type.STRING,
            null,
            Importance.LOW,
            "Optional OAuth2 Client Id. Used by the Worker to identify itself at an Authorization Server. Client Credentials Flow."
        )
        .define(
            parseKafkaConnectConfig(LHConfig.OAUTH_CLIENT_SECRET_KEY),
            Type.PASSWORD,
            null,
            Importance.LOW,
            "Optional OAuth2 Client Secret. Used by the Worker to identify itself at an Authorization Server. Client Credentials Flow."
        )
        .define(
            parseKafkaConnectConfig(LHConfig.OAUTH_ACCESS_TOKEN_URL_KEY),
            Type.STRING,
            null,
            Importance.LOW,
            "Optional Access Token URL provided by the OAuth Authorization Server. Used by the Worker to obtain a token using client credentials flow."
        )
        .define(
            parseKafkaConnectConfig(LHConfig.CLIENT_CERT_KEY),
            Type.STRING,
            null,
            Importance.LOW,
            "Optional location of Client Cert file for mTLS connection."
        )
        .define(
            parseKafkaConnectConfig(LHConfig.CLIENT_KEY_KEY),
            Type.STRING,
            null,
            Importance.LOW,
            "Optional location of Client Private Key file for mTLS connection."
        )
        .define(
            parseKafkaConnectConfig(LHConfig.CA_CERT_KEY),
            Type.STRING,
            null,
            Importance.LOW,
            "Optional location of CA Cert file that issued the server side certificates. For TLS and mTLS connection."
        )
        .define(
            parseKafkaConnectConfig(LHConfig.GRPC_KEEPALIVE_TIME_MS_KEY),
            Type.LONG,
            Duration.ofSeconds(45).toMillis(),
            Importance.LOW,
            "Time in milliseconds to configure keepalive pings on the grpc client."
        )
        .define(
            parseKafkaConnectConfig(LHConfig.GRPC_KEEPALIVE_TIMEOUT_MS_KEY),
            Type.LONG,
            Duration.ofSeconds(5).toMillis(),
            Importance.LOW,
            "Time in milliseconds to configure the timeout for the keepalive pings on the grpc client."
        );

    public LHSinkConnectorConfig(Map<?, ?> props) {
        super(CONFIG_DEF, props);
    }

    /**
     * Transform LH config (ex: LHC_API_HOST) into a kafka connect config (ex: lhc.api.host)
     * @param key Config in LH format
     * @return Config in kafka connect format
     */
    private static String parseKafkaConnectConfig(String key) {
        return key.replace("_", ".").toLowerCase();
    }

    /**
     * Transform kafka connect config (ex: lhc.api.host) into LH config (ex: LHC_API_HOST)
     * @param key Config in kafka connect format
     * @return Config in LH format
     */
    private static String parseLHConfig(String key) {
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
            CONFIG_DEF.toEnrichedRst();
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
