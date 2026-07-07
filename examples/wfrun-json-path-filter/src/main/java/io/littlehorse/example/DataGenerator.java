package io.littlehorse.example;

import java.util.stream.Stream;

public class DataGenerator {

    public static void main(String[] args) {
        int datasetSize = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        Stream.generate(DataGenerator::newQuote).limit(datasetSize).forEach(System.out::println);
    }

    private static Quote newQuote() {
        String quote = SampleData.starWars().character().quote();
        return Quote.builder()
                .quote(quote)
                .priority(SampleData.bool() ? "high" : "low")
                .build();
    }
}
