package io.littlehorse.example;

import net.datafaker.Faker;
import net.datafaker.service.RandomService;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** A thin {@link Faker} wrapper with helpers shared by the example data generators. */
public final class SampleData {

    private SampleData() {}

    private static final Faker FAKER = new Faker();
    private static final StarWars STAR_WARS = new StarWars();

    // Star Wars films: id, title, director, and a '|'-separated cast list.
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

    /** Random Star Wars data (character names, droids, vehicles, quotes, films, ...). */
    public static StarWars starWars() {
        return STAR_WARS;
    }

    /** A random long between {@code min} (inclusive) and {@code max} (exclusive). */
    public static long numberBetween(long min, long max) {
        return FAKER.number().numberBetween(min, max);
    }

    /** A random double between {@code min} and {@code max} with up to {@code maxDecimals} decimals. */
    public static double numberBetween(long min, long max, int maxDecimals) {
        return FAKER.number().randomDouble(maxDecimals, min, max);
    }

    /** A random boolean. */
    public static boolean bool() {
        return FAKER.bool().bool();
    }

    /** Returns {@code true} with the given probability (a value between 0.0 and 1.0). */
    public static boolean chance(double probability) {
        return random().nextDouble() < probability;
    }

    /** The shared random service. */
    private static RandomService random() {
        return FAKER.random();
    }

    /** A random identifier with no dashes, suitable for keys and correlation ids. */
    public static String newKey() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /** A Star Wars film with its director and cast. */
    public record Film(int id, String title, String director, List<String> cast) {}

    /** Random Star Wars data, backed by {@link Faker} plus curated example data. */
    public static final class StarWars {

        // Two-part Star Wars character names so they split cleanly into first/last name.
        private static final String[] CHARACTERS = {
            "Luke Skywalker",
            "Leia Organa",
            "Han Solo",
            "Darth Vader",
            "Obi-Wan Kenobi",
            "Boba Fett",
            "Poe Dameron",
            "Kylo Ren",
            "Padmé Amidala",
            "Mace Windu",
        };

        private final net.datafaker.providers.entertainment.StarWars delegate = FAKER.starWars();

        private StarWars() {}

        /** A random Star Wars character name split into first and last name. */
        public CharacterName characterName() {
            String[] parts = CHARACTERS[random().nextInt(CHARACTERS.length)].split(" ", 2);
            return new CharacterName(parts[0], parts[1]);
        }

        public String droids() {
            return delegate.droids();
        }

        public String callSign() {
            return delegate.callSign();
        }

        public String vehicles() {
            return delegate.vehicles();
        }

        public String quotes() {
            return delegate.quotes();
        }

        public String species() {
            return delegate.species();
        }

        public String planets() {
            return delegate.planets();
        }

        /** A random Star Wars film with its director and cast. */
        public Film film() {
            String[] film = FILMS[random().nextInt(FILMS.length)];
            return new Film(
                    Integer.parseInt(film[0]),
                    film[1],
                    film[2],
                    Arrays.asList(film[3].split("\\|")));
        }

        /** A Star Wars character's first and last name. */
        public record CharacterName(String firstName, String lastName) {
            /** The character's first and last name joined together. */
            public String fullName() {
                return firstName + " " + lastName;
            }
        }
    }
}
