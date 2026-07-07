package io.littlehorse.example;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

public class DataGenerator {

    public static void main(String[] args) {
        int datasetSize = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        Stream.generate(DataGenerator::newRecord).limit(datasetSize).forEach(System.out::println);
    }

    // The raw records are flat; the JsonPathMapperTransform reshapes them into the nested
    // Pilot/Vehicle struct, so the generator emits the source fields, not the struct.
    private static String newRecord() {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("name", SampleData.starWars().characterName().fullName());
        raw.put("model", SampleData.starWars().vehicles());
        return "%s|%s".formatted(SampleData.newKey(), JsonSerializer.serialize(raw));
    }
}
