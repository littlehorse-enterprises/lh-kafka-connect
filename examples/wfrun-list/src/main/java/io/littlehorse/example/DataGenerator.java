package io.littlehorse.example;

import net.datafaker.Faker;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DataGenerator {

    private static final Faker faker = new Faker();

    public static void main(String[] args) {
        int datasetSize = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        int droidsListSize = args.length > 1 ? Integer.parseInt(args[1]) : 3;
        Stream.generate(() -> newDroidsWrapper(droidsListSize))
                .limit(datasetSize)
                .forEach(System.out::println);
    }

    private static Droids newDroidsWrapper(int total) {
        return Droids.builder().droids(newDroidsList(total)).build();
    }

    public static List<Droid> newDroidsList(int total) {
        return Stream.generate(
                        () -> Droid.builder().name(faker.starWars().droids()).build())
                .limit(total)
                .collect(Collectors.toList());
    }
}
