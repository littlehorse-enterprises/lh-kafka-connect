package io.littlehorse.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Registers the JSON Schema in Apicurio Registry under a custom group and artifact id, then
 * produces records serialized with the Apicurio JSON Schema serializer. The serializer resolves the
 * schema through the explicit {@code apicurio.registry.artifact.group-id} /
 * {@code apicurio.registry.artifact.artifact-id} coordinates rather than the default
 * {@code <topic>-value} strategy.
 *
 * <p>The records carry the planet fields at the top level (no envelope), so the connector relies on
 * a {@code JsonPathMapperTransform} to reshape them into the {@code WfSpec} input variables. Run
 * with {@code -DmainClass=io.littlehorse.example.Producer}.
 */
public class Producer {

    private static final String BOOTSTRAP_SERVERS = "localhost:19092";
    private static final String APICURIO_URL = "http://localhost:8080/apis/registry/v3";
    private static final String TOPIC = "example-wfrun-apicurio-json-schema-json-path";

    // Custom (non-standard) registry coordinates for the schema.
    private static final String GROUP_ID = "star-wars";
    private static final String ARTIFACT_ID = "planet";

    // Envelope-less schema: the planet fields live at the top level of the record value.
    private static final String SCHEMA = """
            {
              "$schema": "http://json-schema.org/draft-07/schema#",
              "title": "Planet",
              "type": "object",
              "properties": {
                "name": { "type": "string" },
                "climate": { "type": "string" },
                "terrain": { "type": "string" },
                "population": { "type": "integer" }
              },
              "required": ["name", "climate", "terrain", "population"]
            }
            """;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) {
        int datasetSize = args.length > 0 ? Integer.parseInt(args[0]) : 10;

        new ApicurioRegistry(APICURIO_URL).register(GROUP_ID, ARTIFACT_ID, SCHEMA);

        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        config.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                "io.apicurio.registry.serde.jsonschema.JsonSchemaKafkaSerializer");
        config.put("apicurio.registry.url", APICURIO_URL);
        config.put("apicurio.registry.auto-register", "false");
        config.put("apicurio.registry.find-latest", "true");
        // Resolve the schema through the custom coordinates instead of the default strategy.
        config.put("apicurio.registry.artifact.group-id", GROUP_ID);
        config.put("apicurio.registry.artifact.artifact-id", ARTIFACT_ID);

        try (KafkaProducer<String, JsonNode> producer = new KafkaProducer<>(config)) {
            for (int i = 0; i < datasetSize; i++) {
                SampleData.StarWars.Planet planet = SampleData.starWars().planet();
                JsonNode value = MAPPER.valueToTree(planet);
                producer.send(new ProducerRecord<>(TOPIC, null, null, value));
                System.out.println("Produced: " + value);
            }
            producer.flush();
        }
    }
}
