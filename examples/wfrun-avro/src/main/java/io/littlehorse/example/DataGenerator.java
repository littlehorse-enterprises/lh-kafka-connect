package io.littlehorse.example;

import java.util.stream.Stream;

public class DataGenerator {

    public static void main(String[] args) {
        int datasetSize = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        Stream.generate(DataGenerator::newPlanet).limit(datasetSize).forEach(System.out::println);
    }

    private static Planet newPlanet() {
        return Planet.builder()
                .name(SampleData.starWars().planets())
                .population(SampleData.numberBetween(1_000_000L, 10_000_000L))
                .build();
    }
}
