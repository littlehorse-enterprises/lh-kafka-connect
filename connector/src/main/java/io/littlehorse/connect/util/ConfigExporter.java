package io.littlehorse.connect.util;

import io.littlehorse.connect.ExternalEventSinkConnectorConfig;
import io.littlehorse.connect.WfRunSinkConnectorConfig;
import io.littlehorse.connect.predicate.FilterByFieldPredicateConfig;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigExporter {

    private ConfigExporter() {}

    private static final List<Section> SECTIONS = List.of(
            Section.builder()
                    .title("WfRunSinkConnector Configurations")
                    .content(WfRunSinkConnectorConfig.CONFIG_DEF.toEnrichedRst())
                    .build(),
            Section.builder()
                    .title("ExternalEventSinkConnector Configurations")
                    .content(ExternalEventSinkConnectorConfig.CONFIG_DEF.toEnrichedRst())
                    .build(),
            Section.builder()
                    .title("FilterByFieldPredicate Configurations")
                    .content(FilterByFieldPredicateConfig.CONFIG_DEF.toEnrichedRst())
                    .build());

    @Builder
    @Getter
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Section {
        private String title;
        private String content;

        @Override
        public String toString() {
            return String.format("## %s\n\n%s", title.strip(), content.strip());
        }
    }

    public static String toEnrichedRst() {
        return "# lh-kafka-connect\n\nLittleHorse Connectors for Kafka Connect.\n\n"
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
