package io.littlehorse.connect.util;

import io.littlehorse.connect.ExternalEventSinkConnectorConfig;
import io.littlehorse.connect.WfRunSinkConnectorConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigExporter {

    private ConfigExporter() {}

    public static String toEnrichedRst() {
        return "# lh-kafka-connect" + "\n\n"
                + "LittleHorse Sink Connectors for Kafka Connect."
                + "\n\n"
                + "## WfRunSinkConnector Configurations"
                + "\n\n"
                + WfRunSinkConnectorConfig.CONFIG_DEF.toEnrichedRst().strip()
                + "\n\n"
                + "## ExternalEventSinkConnector Configurations"
                + "\n\n"
                + ExternalEventSinkConnectorConfig.CONFIG_DEF.toEnrichedRst().strip()
                + "\n";
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Path was expected but not provided");
            System.exit(1);
        }

        Files.writeString(Path.of(args[0]), toEnrichedRst());
    }
}
