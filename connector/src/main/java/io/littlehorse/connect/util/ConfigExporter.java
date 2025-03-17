package io.littlehorse.connect.util;

import io.littlehorse.connect.ExternalEventSinkConnector;
import io.littlehorse.connect.ExternalEventSinkConnectorConfig;
import io.littlehorse.connect.WfRunSinkConnector;
import io.littlehorse.connect.WfRunSinkConnectorConfig;
import io.littlehorse.connect.predicate.FilterByFieldPredicate;
import io.littlehorse.connect.predicate.FilterByFieldPredicateConfig;

import lombok.Builder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigExporter {

    private ConfigExporter() {}

    private static final List<Section> SECTIONS = List.of(
            Section.builder()
                    .title(WfRunSinkConnector.class.getSimpleName())
                    .content(WfRunSinkConnectorConfig.CONFIG_DEF.toEnrichedRst())
                    .build(),
            Section.builder()
                    .title(ExternalEventSinkConnector.class.getSimpleName())
                    .content(ExternalEventSinkConnectorConfig.CONFIG_DEF.toEnrichedRst())
                    .build(),
            Section.builder()
                    .title(FilterByFieldPredicate.class.getSimpleName())
                    .content(FilterByFieldPredicateConfig.CONFIG_DEF.toEnrichedRst())
                    .build());

    @Builder
    public static class Section {
        private String title;
        private String content;

        @Override
        public String toString() {
            return String.format("## %s Configurations\n\n%s", title.strip(), content.strip());
        }
    }

    public static String toEnrichedRst() {
        return "# LittleHorse Connectors for Kafka Connect\n\n"
                + SECTIONS.stream().map(Section::toString).collect(Collectors.joining("\n\n"))
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
