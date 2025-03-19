package io.littlehorse.example;

import net.datafaker.Faker;

import java.util.stream.Stream;

public class DataGenerator {

    private static final Faker faker = new Faker();

    public static void main(String[] args) {
        int datasetSize = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        Stream.generate(DataGenerator::newPlanet)
                .map(JsonSerializer::serialize)
                .limit(datasetSize)
                .forEach(System.out::println);
    }

    private static Species newPlanet() {
        return Species.newBuilder().setName(faker.starWars().species()).build();
    }
}
