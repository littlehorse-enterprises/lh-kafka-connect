package io.littlehorse.example;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DataGenerator {

    public static void main(String[] args) {
        int datasetSize = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        int squadronSize = args.length > 1 ? Integer.parseInt(args[1]) : 4;
        Stream.generate(() -> newSquadron(squadronSize))
                .limit(datasetSize)
                .forEach(System.out::println);
    }

    private static Squadron newSquadron(int squadronSize) {
        return Squadron.builder()
                .id(SampleData.newKey())
                .units(newSquadronUnitList(squadronSize))
                .build();
    }

    private static List<SquadronUnit> newSquadronUnitList(int members) {
        return Stream.generate(() -> {
                    SampleData.StarWars.Pilot pilot = SampleData.starWars().pilot();
                    return SquadronUnit.builder()
                            .callSign(pilot.callSign())
                            .pilot(pilot.name())
                            .build();
                })
                .limit(members)
                .collect(Collectors.toList());
    }
}
