package io.littlehorse.example;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DataGenerator {

    public static void main(String[] args) {
        int datasetSize = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        int squadronSize = args.length > 1 ? Integer.parseInt(args[1]) : 4;
        Stream.generate(() -> "%s|%s".formatted(SampleData.newKey(), newSquadron(squadronSize)))
                .limit(datasetSize)
                .forEach(System.out::println);
    }

    private static List<SquadronUnit> newSquadron(int members) {
        return Stream.generate(() -> SquadronUnit.builder()
                        .callSign(SampleData.starWars().callSign())
                        .build())
                .limit(members)
                .collect(Collectors.toList());
    }
}
