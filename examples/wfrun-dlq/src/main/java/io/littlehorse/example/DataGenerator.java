package io.littlehorse.example;

import java.util.stream.Stream;

public class DataGenerator {

    public static void main(String[] args) {
        int datasetSize = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        Stream.generate(DataGenerator::newCharacter)
                .limit(datasetSize)
                .forEach(System.out::println);
    }

    private static String maybeAddError() {
        return SampleData.chance(0.2) ? "This description will cause the connector to fail" : null;
    }

    private static Character newCharacter() {
        return Character.builder()
                .name(SampleData.starWars().characterName().fullName())
                .description(maybeAddError())
                .build();
    }
}
