package io.littlehorse.example;

import net.datafaker.Faker;

import java.util.UUID;
import java.util.stream.Stream;

public class DataGenerator {

    private static final Faker faker = new Faker();

    public static void main(String[] args) {
        int datasetSize = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        Stream.generate(DataGenerator::newPayment).limit(datasetSize).forEach(System.out::println);
    }

    private static String newKey() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String newPayment() {
        return "%s|%s".formatted(newKey(), newDroid());
    }

    private static Droid newDroid() {
        return Droid.builder()
                .name(faker.starWars().droids())
                .credits(faker.number().numberBetween(1_000L, 10_000L))
                .build();
    }
}
