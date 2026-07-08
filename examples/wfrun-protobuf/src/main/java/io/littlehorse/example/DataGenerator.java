package io.littlehorse.example;

import java.util.stream.Stream;

public class DataGenerator {

    public static void main(String[] args) {
        int datasetSize = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        Stream.generate(DataGenerator::newPlanet)
                .map(ProtobufSerializer::serialize)
                .limit(datasetSize)
                .forEach(System.out::println);
    }

    private static Species newPlanet() {
        return Species.newBuilder()
                .setName(SampleData.starWars().species().name())
                .build();
    }
}
