package io.littlehorse.example;

import java.util.stream.Stream;

public class DataGenerator {

    public static void main(String[] args) {
        int datasetSize = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        Stream.generate(DataGenerator::newPayment).limit(datasetSize).forEach(System.out::println);
    }

    private static Payment newPayment() {
        return Payment.builder()
                .id(SampleData.newKey())
                .droid(SampleData.starWars().droid().name())
                .credits(SampleData.numberBetween(1_000L, 10_000L, 2))
                .build();
    }
}
