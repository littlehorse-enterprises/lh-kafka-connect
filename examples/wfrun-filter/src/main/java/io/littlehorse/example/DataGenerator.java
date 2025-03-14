package io.littlehorse.example;

import static io.littlehorse.example.Main.WF_NAME;

import net.datafaker.Faker;

import java.util.UUID;
import java.util.stream.Stream;

public class DataGenerator {

    private static final Faker faker = new Faker();

    public static void main(String[] args) {
        int datasetSize = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        Stream.generate(() -> "%s|%s".formatted(newQuoteKey(), newQuote()))
                .limit(datasetSize)
                .forEach(System.out::println);
    }

    private static QuoteKey newQuoteKey() {
        return QuoteKey.builder()
                .id(UUID.randomUUID())
                .wfSpecName(faker.bool().bool() ? WF_NAME : "invalid-wf-spec-name")
                .build();
    }

    private static Quote newQuote() {
        String quote = faker.starWars().quotes();
        return Quote.builder().quote(quote).length(quote.length()).build();
    }
}
