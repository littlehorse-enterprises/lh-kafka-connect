package io.littlehorse.example;

import java.util.Random;
import java.util.UUID;

/** Helpers shared by the example data generators. */
public final class SampleData {

    private SampleData() {}

    private static final Random RANDOM = new Random();

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

    /** A random identifier with no dashes, suitable for keys and correlation ids. */
    public static String newKey() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /** A random Star Wars character name split into first and last name. */
    public static CharacterName characterName() {
        String[] parts = CHARACTERS[RANDOM.nextInt(CHARACTERS.length)].split(" ", 2);
        return new CharacterName(parts[0], parts[1]);
    }

    /** A Star Wars character's first and last name. */
    public record CharacterName(String firstName, String lastName) {}
}
