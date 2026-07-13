package io.littlehorse.example;

import java.util.stream.Stream;

public class DataGenerator {

    public static void main(String[] args) {
        int datasetSize = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        Stream.generate(DataGenerator::newOrder).limit(datasetSize).forEach(System.out::println);
    }

    private static Order newOrder() {
        return Order.builder()
                // A dash-free identifier keeps the derived "order-<id>" a valid hostname.
                .orderId(SampleData.newKey())
                .customer(SampleData.starWars().character().fullName())
                .build();
    }
}
