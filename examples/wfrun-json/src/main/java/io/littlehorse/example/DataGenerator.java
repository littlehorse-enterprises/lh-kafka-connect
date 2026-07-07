package io.littlehorse.example;

import java.util.stream.Stream;

public class DataGenerator {

    public static void main(String[] args) {
        int datasetSize = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        Stream.generate(DataGenerator::newCharacter)
                .limit(datasetSize)
                .forEach(System.out::println);
    }

    private static Character newCharacter() {
        SampleData.StarWars.Pilot source = SampleData.starWars().pilot();
        return Character.builder()
                .name(source.name())
                .vehicle(Vehicle.builder().model(source.vehicle()).build())
                .build();
    }
}
