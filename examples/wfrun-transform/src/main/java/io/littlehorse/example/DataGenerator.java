package io.littlehorse.example;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

public class DataGenerator {

    public static void main(String[] args) {
        int datasetSize = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        Stream.generate(DataGenerator::newFilm).limit(datasetSize).forEach(System.out::println);
    }

    // The raw records are flat; the JsonPathMapperTransform reshapes them into the nested
    // Film/Director struct, so the generator emits the source fields, not the struct. The
    // cast list is mapped to the JSON_ARR cast variable.
    private static String newFilm() {
        SampleData.Film film = SampleData.starWars().film();
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("id", film.id());
        raw.put("title", film.title());
        raw.put("director", film.director());
        raw.put("domesticGross", SampleData.numberBetween(100, 1000, 2));
        raw.put("internationalGross", SampleData.numberBetween(100, 1000, 2));
        raw.put("cast", film.cast());
        return JsonSerializer.serialize(raw);
    }
}
