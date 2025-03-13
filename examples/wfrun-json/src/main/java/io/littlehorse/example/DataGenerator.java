package io.littlehorse.example;

import net.datafaker.Faker;

import java.util.stream.Stream;

public class DataGenerator {

    private static final Faker faker = new Faker();

    public static void main(String[] args) {
        Stream.generate(() -> Person.builder()
                        .name(faker.starWars().character())
                        .vehicle(Vehicle.builder()
                                .model(faker.starWars().vehicles())
                                .build())
                        .build())
                .limit(args.length > 0 ? Integer.parseInt(args[0]) : 10)
                .forEach(System.out::println);
    }
}
