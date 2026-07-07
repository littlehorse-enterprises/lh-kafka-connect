package io.littlehorse.example;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

public class DataGenerator {

    public static void main(String[] args) {
        int datasetSize = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        Stream.generate(DataGenerator::newRecord).limit(datasetSize).forEach(System.out::println);
    }

    // The raw records carry flat firstName/lastName fields; the JsonPathMapperTransform
    // concatenates them into the single STR content the correlated event posts.
    private static String newRecord() {
        SampleData.StarWars.Character name = SampleData.starWars().character();
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("firstName", name.firstName());
        raw.put("lastName", name.lastName());
        return "%s|%s".formatted(SampleData.newKey(), JsonSerializer.serialize(raw));
    }
}
