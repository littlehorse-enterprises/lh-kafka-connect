package io.littlehorse.connect.util;

import io.littlehorse.connect.ExternalEventSinkConnectorConfig;
import io.littlehorse.connect.WfRunSinkConnectorConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigurationExporter {
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Path was expected but not provided");
            System.exit(1);
        }

        String docBuilder = "# lh-kafka-connect" + "\n\n"
                + "LittleHorse Sink Connector for Kafka Connect"
                + "\n\n"
                + "## WfRunSinkConnector Configurations"
                + "\n\n"
                + WfRunSinkConnectorConfig.CONFIG_DEF.toEnrichedRst().strip()
                + "\n\n"
                + "## ExternalEventSinkConnector Configurations"
                + "\n\n"
                + ExternalEventSinkConnectorConfig.CONFIG_DEF.toEnrichedRst().strip()
                + "\n";

        Files.writeString(Path.of(args[0]), docBuilder);
    }
}
