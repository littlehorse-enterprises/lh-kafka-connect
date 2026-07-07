package io.littlehorse.example;

import java.util.stream.Stream;

public class DataGenerator {

    public static void main(String[] args) {
        int datasetSize = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        Stream.generate(DataGenerator::newPlanet).limit(datasetSize).forEach(System.out::println);
    }

    private static Planet newPlanet() {
        SampleData.StarWars.Planet source = SampleData.starWars().planet();
        return Planet.builder()
                .name(source.name())
                .population(source.population())
                .build();
    }
}
