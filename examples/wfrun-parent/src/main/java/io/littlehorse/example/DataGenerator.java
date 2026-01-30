package io.littlehorse.example;

import net.datafaker.Faker;

import java.util.UUID;
import java.util.stream.Stream;

public class DataGenerator {

    private static final Faker faker = new Faker();

    public static void main(String[] args) {
        int datasetSize = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        Stream.generate(DataGenerator::newRecord).limit(datasetSize).forEach(System.out::println);
    }

    private static ChildWfRunData newRecord() {
        return ChildWfRunData.builder()
                .id("custom-id-" + UUID.randomUUID())
                .parentId("parent-wf-run-id")
                .build();
    }
}
