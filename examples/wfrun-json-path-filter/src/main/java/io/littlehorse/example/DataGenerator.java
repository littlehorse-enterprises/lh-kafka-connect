package io.littlehorse.example;

import net.datafaker.Faker;

import java.util.stream.Stream;

public class DataGenerator {

    private static final Faker faker = new Faker();

    public static void main(String[] args) {
        int datasetSize = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        Stream.generate(DataGenerator::newQuote).limit(datasetSize).forEach(System.out::println);
    }

    private static Quote newQuote() {
        String quote = faker.starWars().quotes();
        return Quote.builder()
                .quote(quote)
                .priority(faker.bool().bool() ? "high" : "low")
                .build();
    }
}
