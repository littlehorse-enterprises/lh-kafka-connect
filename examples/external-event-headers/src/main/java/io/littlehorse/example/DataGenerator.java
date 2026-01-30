package io.littlehorse.example;

import net.datafaker.Faker;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DataGenerator {

    private static final Faker faker = new Faker();

    public static void main(String[] args) {
        int datasetSize = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        int squadronSize = args.length > 1 ? Integer.parseInt(args[1]) : 4;
        Stream.generate(() -> newSquadron(squadronSize))
                .limit(datasetSize)
                .forEach(System.out::println);
    }

    private static Squadron newSquadron(int squadronSize) {
        return Squadron.builder()
                .id(newKey())
                .units(newSquadronUnitList(squadronSize))
                .build();
    }

    private static String newKey() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static List<SquadronUnit> newSquadronUnitList(int members) {
        return Stream.generate(() -> SquadronUnit.builder()
                        .callSign(faker.starWars().callSign())
                        .pilot(faker.starWars().character())
                        .build())
                .limit(members)
                .collect(Collectors.toList());
    }
}
