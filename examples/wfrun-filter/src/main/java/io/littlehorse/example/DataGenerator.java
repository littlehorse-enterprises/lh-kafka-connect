package io.littlehorse.example;

import static io.littlehorse.example.Main.WF_NAME;

import java.util.stream.Stream;

public class DataGenerator {

    public static void main(String[] args) {
        int datasetSize = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        Stream.generate(() -> "%s|%s".formatted(newQuoteKey(), newQuote()))
                .limit(datasetSize)
                .forEach(System.out::println);
    }

    private static QuoteKey newQuoteKey() {
        return QuoteKey.builder()
                .id(SampleData.newKey())
                .wfSpecName(SampleData.bool() ? WF_NAME : "invalid-wf-spec-name")
                .build();
    }

    private static Quote newQuote() {
        String quote = SampleData.starWars().character().quote();
        return Quote.builder().quote(quote).length(quote.length()).build();
    }
}
