package io.littlehorse.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registers a referenced JSON Schema in Apicurio Registry and produces records serialized with the
 * Apicurio JSON Schema serializer. The value schema {@code $ref}s a separate {@code vehicle} schema
 * artifact, demonstrating JSON Schema references. Run with
 * {@code -DmainClass=io.littlehorse.example.Producer}.
 */
public class Producer {

    private static final String BOOTSTRAP_SERVERS = "localhost:19092";
    private static final String APICURIO_URL = "http://localhost:8080/apis/registry/v3";
    private static final String TOPIC = "example-wfrun-apicurio-json-schema-reference";
    private static final String ARTIFACT_ID = TOPIC + "-value";
    private static final String VEHICLE_ARTIFACT_ID = "example-vehicle";
    private static final String VEHICLE_REF = "https://littlehorse.io/schemas/vehicle.json";

    // Standalone schema that the value schema references.
    private static final String VEHICLE_SCHEMA = """
            {
              "$id": "https://littlehorse.io/schemas/vehicle.json",
              "$schema": "http://json-schema.org/draft-07/schema#",
              "title": "Vehicle",
              "type": "object",
              "properties": {
                "model": { "type": "string" }
              },
              "required": ["model"]
            }
            """;

    // Value schema referencing the vehicle schema via $ref.
    private static final String PILOT_SCHEMA = """
            {
              "$schema": "http://json-schema.org/draft-07/schema#",
              "title": "Pilot envelope",
              "type": "object",
              "properties": {
                "pilot": {
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" },
                    "vehicle": { "$ref": "https://littlehorse.io/schemas/vehicle.json" }
                  },
                  "required": ["name", "vehicle"]
                }
              },
              "required": ["pilot"]
            }
            """;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) {
        int datasetSize = args.length > 0 ? Integer.parseInt(args[0]) : 10;

        ApicurioRegistry registry = new ApicurioRegistry(APICURIO_URL);
        // Register the referenced schema first, then the value schema that references it.
        registry.register("default", VEHICLE_ARTIFACT_ID, VEHICLE_SCHEMA);
        registry.register(
                "default",
                ARTIFACT_ID,
                PILOT_SCHEMA,
                List.of(new ApicurioRegistry.Reference(
                        VEHICLE_REF, "default", VEHICLE_ARTIFACT_ID, "1")));

        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        config.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                "io.apicurio.registry.serde.jsonschema.JsonSchemaKafkaSerializer");
        config.put("apicurio.registry.url", APICURIO_URL);
        config.put("apicurio.registry.auto-register", "false");

        try (KafkaProducer<String, JsonNode> producer = new KafkaProducer<>(config)) {
            for (int i = 0; i < datasetSize; i++) {
                Pilot pilot = Pilot.builder()
                        .name(SampleData.starWars().characterName().fullName())
                        .vehicle(Vehicle.builder()
                                .model(SampleData.starWars().vehicles())
                                .build())
                        .build();
                JsonNode value = MAPPER.valueToTree(Map.of(Main.VARIABLE_PILOT, pilot));
                producer.send(new ProducerRecord<>(TOPIC, null, null, value));
                System.out.println("Produced: " + value);
            }
            producer.flush();
        }
    }
}
