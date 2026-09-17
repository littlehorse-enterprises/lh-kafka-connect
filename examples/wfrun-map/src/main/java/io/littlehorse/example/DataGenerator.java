package io.littlehorse.example;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

public class DataGenerator {

    public static void main(String[] args) {
        int datasetSize = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        int inventorySize = args.length > 1 ? Integer.parseInt(args[1]) : 3;
        if (inventorySize < 1) {
            throw new IllegalArgumentException("Inventory size must be at least 1");
        }

        Stream.generate(() -> newInventoryRecord(inventorySize))
                .limit(datasetSize)
                .map(JsonSerializer::serialize)
                .forEach(System.out::println);
    }

    private static Map<String, Object> newInventoryRecord(int inventorySize) {
        int firstProductId = Math.toIntExact(SampleData.numberBetween(100, 10_000));
        Map<Integer, String> inventory = new LinkedHashMap<>();
        for (int index = 0; index < inventorySize; index++) {
            inventory.put(firstProductId + index, SampleData.starWars().droid().name());
        }

        int requestedIndex = Math.toIntExact(SampleData.numberBetween(0, inventorySize));
        Map<String, Object> record = new LinkedHashMap<>();
        record.put(Main.VARIABLE_INVENTORY, inventory);
        record.put(Main.VARIABLE_REQUESTED_PRODUCT_ID, firstProductId + requestedIndex);
        return record;
    }
}
