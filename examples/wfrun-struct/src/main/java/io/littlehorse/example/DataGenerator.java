package io.littlehorse.example;

import java.util.stream.Stream;

public class DataGenerator {

    public static void main(String[] args) {
        int datasetSize = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        Stream.generate(DataGenerator::newPilot).limit(datasetSize).forEach(System.out::println);
    }

    private static Pilot newPilot() {
        SampleData.StarWars.Pilot source = SampleData.starWars().pilot();
        return Pilot.builder()
                .name(source.name())
                .vehicle(Vehicle.builder().model(source.vehicle()).build())
                .build();
    }
}
