package io.littlehorse.example;

import net.datafaker.Faker;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

public class DataGenerator {

    private static final Faker faker = new Faker();

    public static void main(String[] args) {
        int datasetSize = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        Stream.generate(DataGenerator::newRecord).limit(datasetSize).forEach(System.out::println);
    }

    private static String newKey() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    // The raw records carry flat firstName/lastName fields; the JsonPathMapperTransform
    // concatenates them into the single STR content the correlated event posts.
    private static String newRecord() {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("firstName", faker.name().firstName());
        raw.put("lastName", faker.name().lastName());
        return "%s|%s".formatted(newKey(), JsonSerializer.serialize(raw));
    }
}
