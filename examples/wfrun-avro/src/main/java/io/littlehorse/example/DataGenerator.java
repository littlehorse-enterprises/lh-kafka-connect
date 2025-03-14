package io.littlehorse.example;

import net.datafaker.Faker;

import java.util.stream.Stream;

public class DataGenerator {

    private static final Faker faker = new Faker();

    public static void main(String[] args) {
        Stream.generate(DataGenerator::newPlanet)
                .limit(args.length > 0 ? Integer.parseInt(args[0]) : 10)
                .forEach(System.out::println);
    }

    private static Planet newPlanet() {
        return Planet.builder()
                .name(faker.starWars().planets())
                .population(faker.number().numberBetween(1_000_000L, 10_000_000L))
                .build();
    }
}
