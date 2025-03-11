package io.littlehorse.example;

import java.util.stream.Stream;
import net.datafaker.Faker;

public class DataGenerator {

    private static final Faker faker = new Faker();

    // ./gradlew -q example-wfrun-dlq:run -DmainClass="io.littlehorse.example.DataGenerator" --args="10" > examples/wfrun-dlq/data.txt
    public static void main(String[] args) {
        Stream
            .generate(() ->
                Person.builder().name(faker.starWars().character()).build()
            )
            .limit(args.length > 0 ? Integer.parseInt(args[0]) : 10)
            .forEach(person ->
                System.out.println(
                    (Math.random() < 0.1 ? "ERROR" : "") + person
                )
            );
    }
}
