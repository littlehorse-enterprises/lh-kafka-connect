package io.littlehorse.example;

import java.util.stream.Stream;

public class DataGenerator {

    public static void main(String[] args) {
        int datasetSize = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        Stream.generate(DataGenerator::newPayment).limit(datasetSize).forEach(System.out::println);
    }

    private static String newPayment() {
        return "%s|%s".formatted(SampleData.newKey(), newDroid());
    }

    private static Droid newDroid() {
        return Droid.builder()
                .name(SampleData.starWars().droids())
                .credits(SampleData.numberBetween(1_000L, 10_000L, 2))
                .build();
    }
}
