package io.littlehorse.example;

import net.datafaker.Faker;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class DataGenerator {

    private static final Faker faker = new Faker();

    // The raw records are flat; the JsonPathMapperTransform reshapes them into the nested
    // Film/Director struct, so the generator emits the source fields, not the struct. The
    // fourth column is a '|'-separated cast list mapped to the JSON_ARR cast variable.
    private static final String[][] FILMS = {
        {"1", "The Phantom Menace", "George Lucas", "Liam Neeson|Ewan McGregor|Natalie Portman"},
        {
            "2",
            "Attack of the Clones",
            "George Lucas",
            "Ewan McGregor|Natalie Portman|Hayden Christensen"
        },
        {
            "3",
            "Revenge of the Sith",
            "George Lucas",
            "Ewan McGregor|Natalie Portman|Hayden Christensen"
        },
        {"4", "A New Hope", "George Lucas", "Mark Hamill|Harrison Ford|Carrie Fisher"},
        {"5", "The Empire Strikes Back", "Irvin Kershner", "Mark Hamill|Harrison Ford|Carrie Fisher"
        },
        {"6", "Return of the Jedi", "Richard Marquand", "Mark Hamill|Harrison Ford|Carrie Fisher"},
        {"7", "The Force Awakens", "J.J. Abrams", "Daisy Ridley|John Boyega|Harrison Ford"},
        {"8", "The Last Jedi", "Rian Johnson", "Daisy Ridley|John Boyega|Mark Hamill"},
        {"9", "The Rise of Skywalker", "J.J. Abrams", "Daisy Ridley|John Boyega|Adam Driver"},
    };

    public static void main(String[] args) {
        int datasetSize = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        Stream.generate(DataGenerator::newFilm).limit(datasetSize).forEach(System.out::println);
    }

    private static String newFilm() {
        String[] film = FILMS[faker.random().nextInt(FILMS.length)];
        List<String> cast = Arrays.asList(film[3].split("\\|"));
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("id", Integer.parseInt(film[0]));
        raw.put("title", film[1]);
        raw.put("director", film[2]);
        raw.put("domesticGross", faker.number().randomDouble(2, 100, 1000));
        raw.put("internationalGross", faker.number().randomDouble(2, 100, 1000));
        raw.put("cast", cast);
        return JsonSerializer.serialize(raw);
    }
}
