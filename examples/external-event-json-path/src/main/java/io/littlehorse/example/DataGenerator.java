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

    // The raw records are flat; the JsonPathMapperTransform reshapes them into the nested
    // Pilot/Vehicle struct, so the generator emits the source fields, not the struct.
    private static String newRecord() {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("name", faker.starWars().character());
        raw.put("model", faker.starWars().vehicles());
        return "%s|%s".formatted(newKey(), JsonSerializer.serialize(raw));
    }
}
