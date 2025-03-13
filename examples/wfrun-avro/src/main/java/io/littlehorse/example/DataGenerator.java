package io.littlehorse.example;

import net.datafaker.Faker;

import java.util.stream.Stream;

public class DataGenerator {

    private static final Faker faker = new Faker();

    // ./gradlew -q example-wfrun-avro:run -DmainClass="io.littlehorse.example.DataGenerator"
    // --args="10" > examples/wfrun-avro/data.txt
    public static void main(String[] args) {
        Stream.generate(() -> Planet.builder()
                        .name(faker.starWars().planets())
                        .population(faker.number().numberBetween(1_000_000L, 10_000_000L))
                        .build())
                .limit(args.length > 0 ? Integer.parseInt(args[0]) : 10)
                .forEach(System.out::println);
    }
}
