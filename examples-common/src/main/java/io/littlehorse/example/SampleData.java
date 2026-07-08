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

    /** Random Star Wars data (characters, droids, pilots, films, planets, ...). */
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

    /** Random Star Wars data, backed by {@link Faker} plus curated example data. */
    public static final class StarWars {

        // Star Wars characters: first name, last name, and a signature quote.
        private static final String[][] CHARACTERS = {
            {"Luke", "Skywalker", "I am a Jedi, like my father before me."},
            {"Leia", "Organa", "Help me, Obi-Wan Kenobi. You're my only hope."},
            {"Han", "Solo", "Never tell me the odds."},
            {"Darth", "Vader", "I find your lack of faith disturbing."},
            {"Obi-Wan", "Kenobi", "These aren't the droids you're looking for."},
            {"Boba", "Fett", "He's no good to me dead."},
            {
                "Poe",
                "Dameron",
                "We are the spark that'll light the fire that'll burn the First Order down."
            },
            {"Kylo", "Ren", "Let the past die. Kill it if you have to."},
            {"Padmé", "Amidala", "So this is how liberty dies. With thunderous applause."},
            {"Mace", "Windu", "This party's over."},
            {"Qui-Gon", "Jinn", "Your focus determines your reality."},
            {"Lando", "Calrissian", "This deal is getting worse all the time."},
            {"Wedge", "Antilles", "Look at the size of that thing!"},
            {"Count", "Dooku", "Twice the pride, double the fall."},
            {"Jyn", "Erso", "Rebellions are built on hope."},
            {"Cassian", "Andor", "I've been in this fight since I was six years old."},
            {"Wilhuff", "Tarkin", "You may fire when ready."},
            {"Din", "Djarin", "This is the Way."},
            {"Chirrut", "Îmwe", "I am one with the Force. The Force is with me."},
            {"Saw", "Gerrera", "Save the Rebellion! Save the dream!"},
        };

        // Star Wars films: id, title, director, and a '|'-separated cast list.
        private static final String[][] FILMS = {
            {"1", "The Phantom Menace", "George Lucas", "Liam Neeson|Ewan McGregor|Natalie Portman"
            },
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
            {
                "5",
                "The Empire Strikes Back",
                "Irvin Kershner",
                "Mark Hamill|Harrison Ford|Carrie Fisher"
            },
            {
                "6",
                "Return of the Jedi",
                "Richard Marquand",
                "Mark Hamill|Harrison Ford|Carrie Fisher"
            },
            {"7", "The Force Awakens", "J.J. Abrams", "Daisy Ridley|John Boyega|Harrison Ford"},
            {"8", "The Last Jedi", "Rian Johnson", "Daisy Ridley|John Boyega|Mark Hamill"},
            {"9", "The Rise of Skywalker", "J.J. Abrams", "Daisy Ridley|John Boyega|Adam Driver"},
        };

        // Star Wars planets: name, climate, terrain, and population.
        private static final String[][] PLANETS = {
            {"Tatooine", "arid", "desert", "200000"},
            {"Alderaan", "temperate", "grasslands and mountains", "2000000000"},
            {"Hoth", "frozen", "tundra and ice caves", "0"},
            {"Dagobah", "murky", "swamp and jungles", "0"},
            {"Naboo", "temperate", "grassy hills and swamps", "4500000000"},
            {"Coruscant", "temperate", "cityscape and mountains", "1000000000000"},
            {"Endor", "temperate", "forests and mountains", "30000000"},
            {"Kamino", "temperate", "ocean", "1000000000"},
            {"Geonosis", "arid", "rock and desert", "100000000000"},
            {"Mustafar", "hot", "volcanoes and lava rivers", "20000"},
        };

        // Star Wars Force wielders: name, type (SITH or JEDI), and lightsaber color.
        private static final String[][] WIELDERS = {
            {"Luke Skywalker", "JEDI", "green"},
            {"Obi-Wan Kenobi", "JEDI", "blue"},
            {"Yoda", "JEDI", "green"},
            {"Mace Windu", "JEDI", "purple"},
            {"Qui-Gon Jinn", "JEDI", "green"},
            {"Ahsoka Tano", "JEDI", "white"},
            {"Darth Vader", "SITH", "red"},
            {"Darth Maul", "SITH", "red"},
            {"Palpatine", "SITH", "red"},
            {"Count Dooku", "SITH", "red"},
            {"Kylo Ren", "SITH", "red"},
            {"Darth Bane", "SITH", "red"},
        };

        // Star Wars pilots: name, call sign, and vehicle.
        private static final String[][] PILOTS = {
            {"Luke Skywalker", "Red Five", "T-65 X-wing"},
            {"Wedge Antilles", "Red Two", "T-65 X-wing"},
            {"Biggs Darklighter", "Red Three", "T-65 X-wing"},
            {"Jek Porkins", "Red Six", "T-65 X-wing"},
            {"Garven Dreis", "Red Leader", "T-65 X-wing"},
            {"Dutch Vander", "Gold Leader", "BTL-A4 Y-wing"},
            {"Poe Dameron", "Black One", "T-70 X-wing"},
            {"Wes Janson", "Rogue Three", "T-47 airspeeder"},
            {"Zev Senesca", "Rogue Two", "T-47 airspeeder"},
            {"Derek Klivian", "Rogue Four", "T-47 airspeeder"},
        };

        private final net.datafaker.providers.entertainment.StarWars delegate = FAKER.starWars();

        private StarWars() {}

        /** A random Star Wars character with its name and a signature quote. */
        public Character character() {
            String[] character = CHARACTERS[random().nextInt(CHARACTERS.length)];
            return new Character(character[0], character[1], character[2]);
        }

        /** A random Star Wars droid. */
        public Droid droid() {
            return new Droid(delegate.droids());
        }

        /** A random Star Wars pilot with its call sign and vehicle. */
        public Pilot pilot() {
            String[] pilot = PILOTS[random().nextInt(PILOTS.length)];
            return new Pilot(pilot[0], pilot[1], pilot[2]);
        }

        /** A random Star Wars species. */
        public Species species() {
            return new Species(delegate.species());
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

        /** A random Star Wars planet with its climate, terrain, and population. */
        public Planet planet() {
            String[] planet = PLANETS[random().nextInt(PLANETS.length)];
            return new Planet(planet[0], planet[1], planet[2], Long.parseLong(planet[3]));
        }

        /** A random Star Wars Force wielder with its type and lightsaber color. */
        public ForceWielder forceWielder() {
            String[] wielder = WIELDERS[random().nextInt(WIELDERS.length)];
            return new ForceWielder(wielder[0], wielder[1], wielder[2]);
        }

        /** A Star Wars film with its director and cast. */
        public record Film(int id, String title, String director, List<String> cast) {}

        /** A Star Wars planet with its climate, terrain, and population. */
        public record Planet(String name, String climate, String terrain, long population) {}

        /** A Star Wars Force wielder with its type ({@code SITH} or {@code JEDI}) and lightsaber color. */
        public record ForceWielder(String name, String type, String lightsaberColor) {}

        /** A Star Wars character with its name and a signature quote. */
        public record Character(String firstName, String lastName, String quote) {
            /** The character's first and last name joined together. */
            public String fullName() {
                return firstName + " " + lastName;
            }
        }

        /** A Star Wars droid. */
        public record Droid(String name) {}

        /** A Star Wars pilot with its call sign and vehicle. */
        public record Pilot(String name, String callSign, String vehicle) {}

        /** A Star Wars species. */
        public record Species(String name) {}
    }
}
