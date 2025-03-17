package io.littlehorse.example;

import net.datafaker.Faker;

import java.util.stream.Stream;

public class DataGenerator {

    private static final Faker faker = new Faker();

    public static void main(String[] args) {
        int datasetSize = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        Stream.generate(DataGenerator::newCharacter)
                .limit(datasetSize)
                .forEach(System.out::println);
    }

    private static Character newCharacter() {
        return Character.builder()
                .name(faker.starWars().character())
                .vehicle(Vehicle.builder().model(faker.starWars().vehicles()).build())
                .build();
    }
}
