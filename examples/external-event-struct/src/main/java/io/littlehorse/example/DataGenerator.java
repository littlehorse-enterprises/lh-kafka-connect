package io.littlehorse.example;

import java.util.stream.Stream;

public class DataGenerator {

    public static void main(String[] args) {
        int datasetSize = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        Stream.generate(DataGenerator::newRecord).limit(datasetSize).forEach(System.out::println);
    }

    private static String newRecord() {
        SampleData.StarWars.Pilot source = SampleData.starWars().pilot();
        Pilot pilot = Pilot.builder()
                .name(source.name())
                .vehicle(Vehicle.builder().model(source.vehicle()).build())
                .build();
        return "%s|%s".formatted(SampleData.newKey(), pilot);
    }
}
