package io.littlehorse.kafka.connect;

import io.littlehorse.sdk.common.config.LHConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;

public class LHSinkConnectorConfig extends AbstractConfig {

    public static final ConfigDef CONFIG_DEF = new ConfigDef()
        .define(
            parseConfigKey(LHConfig.API_HOST_KEY),
            Type.STRING,
            Importance.HIGH,
            "LH server hostname"
        )
        .define(
            parseConfigKey(LHConfig.API_PORT_KEY),
            Type.INT,
            Importance.HIGH,
            "LH server port"
        )
        .define(
            parseConfigKey(LHConfig.TENANT_ID_KEY),
            Type.STRING,
            "default",
            Importance.MEDIUM,
            "LH tenant"
        );

    public LHSinkConnectorConfig(Map<?, ?> props) {
        super(CONFIG_DEF, props);
    }

    private static String parseConfigKey(String key) {
        return key.replace("_", ".").toLowerCase();
    }

    public LHConfig toLHConfig() {
        return new LHConfig(toMap());
    }

    private Map<String, Object> toMap() {
        return nonInternalValues()
            .keySet()
            .stream()
            .collect(
                Collectors.toMap(
                    key -> key.toUpperCase().replace(".", "_"),
                    this::get
                )
            );
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
}
