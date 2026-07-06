package io.littlehorse.example;

/**
 * Registers the JSON Schema used by both the source and sink connectors in this example. The
 * {@code FileStreamSourceConnector} emits string values, so the schema is a simple JSON string
 * schema. Run with the default main class:
 *
 * <pre>./gradlew example-apicurio-json-schema-source-sink:run</pre>
 */
public class RegisterSchema {

    private static final String APICURIO_URL = "http://localhost:8080/apis/registry/v3";
    public static final String TOPIC = "example-apicurio-source-sink";
    public static final String ARTIFACT_ID = TOPIC + "-value";

    private static final String SCHEMA = """
            {
              "$schema": "http://json-schema.org/draft-07/schema#",
              "title": "Line",
              "type": "string"
            }
            """;

    public static void main(String[] args) {
        new ApicurioRegistry(APICURIO_URL).register("default", ARTIFACT_ID, SCHEMA);
        System.out.println("Registered JSON Schema artifact: " + ARTIFACT_ID);
    }
}
