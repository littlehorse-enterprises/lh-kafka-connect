package io.littlehorse.example;

import net.datafaker.Faker;

import java.util.stream.Stream;

public class DataGenerator {

    private static final Faker faker = new Faker();

    public static void main(String[] args) {
        int datasetSize = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        Stream.generate(DataGenerator::newPayment).limit(datasetSize).forEach(System.out::println);
    }

    private static Payment newPayment() {
        return Payment.builder()
                .id(SampleData.newKey())
                .droid(faker.starWars().droids())
                .credits(faker.number().numberBetween(1_000L, 10_000L))
                .build();
    }
}
