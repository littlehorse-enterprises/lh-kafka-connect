package io.littlehorse.example;

import net.datafaker.Faker;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DataGenerator {

    private static final Faker faker = new Faker();

    public static void main(String[] args) {
        Stream.generate(() -> Droids.builder()
                        .droids(generateDroids(args.length > 1 ? Integer.parseInt(args[1]) : 3))
                        .build())
                .limit(args.length > 0 ? Integer.parseInt(args[0]) : 10)
                .forEach(System.out::println);
    }

    public static List<Droid> generateDroids(int total) {
        return Stream.generate(
                        () -> Droid.builder().name(faker.starWars().droids()).build())
                .limit(total)
                .collect(Collectors.toList());
    }
}
