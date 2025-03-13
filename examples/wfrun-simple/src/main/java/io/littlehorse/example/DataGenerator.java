package io.littlehorse.example;

import net.datafaker.Faker;

import java.util.stream.Stream;

public class DataGenerator {

    private static final Faker faker = new Faker();

    // ./gradlew -q example-wfrun-simple:run -DmainClass="io.littlehorse.example.DataGenerator"
    // --args="10" > examples/wfrun-simple/data.txt
    public static void main(String[] args) {
        Stream.generate(() -> faker.starWars().character())
                .limit(args.length > 0 ? Integer.parseInt(args[0]) : 10)
                .forEach(System.out::println);
    }
}
