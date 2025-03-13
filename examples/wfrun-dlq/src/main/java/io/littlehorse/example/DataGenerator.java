package io.littlehorse.example;

import net.datafaker.Faker;

import java.util.stream.Stream;

public class DataGenerator {

    private static final Faker faker = new Faker();

    public static void main(String[] args) {
        Stream.generate(() ->
                        Person.builder().name(faker.starWars().character()).build())
                .limit(args.length > 0 ? Integer.parseInt(args[0]) : 10)
                .forEach(person ->
                        System.out.println((Math.random() < 0.1 ? "ERROR" : "") + person));
    }
}
