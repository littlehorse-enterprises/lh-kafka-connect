package io.littlehorse.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.datafaker.Faker;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registers a referenced JSON Schema in Apicurio Registry and produces records serialized with the
 * Apicurio JSON Schema serializer. The value schema {@code $ref}s a separate {@code address} schema
 * artifact, demonstrating JSON Schema references. Run with
 * {@code -DmainClass=io.littlehorse.example.Producer}.
 */
public class Producer {

    private static final String BOOTSTRAP_SERVERS = "localhost:19092";
    private static final String APICURIO_URL = "http://localhost:8080/apis/registry/v3";

    // Standalone schema that the value schema references.
    private static final String ADDRESS_SCHEMA = """
            {
              "$id": "https://littlehorse.io/schemas/address.json",
              "$schema": "http://json-schema.org/draft-07/schema#",
              "title": "Address",
              "type": "object",
              "properties": {
                "street": { "type": "string" },
                "city": { "type": "string" }
              },
              "required": ["street", "city"]
            }
            """;

    // Value schema referencing the address schema via $ref.
    private static final String PERSON_SCHEMA = """
            {
              "$schema": "http://json-schema.org/draft-07/schema#",
              "title": "Person envelope",
              "type": "object",
              "properties": {
                "person": {
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" },
                    "address": { "$ref": "https://littlehorse.io/schemas/address.json" }
                  },
                  "required": ["name", "address"]
                }
              },
              "required": ["person"]
            }
            """;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Faker FAKER = new Faker();

    public static void main(String[] args) {
        int datasetSize = args.length > 0 ? Integer.parseInt(args[0]) : 10;

        ApicurioRegistry registry = new ApicurioRegistry(APICURIO_URL);
        // Register the referenced schema first, then the value schema that references it.
        registry.register("default", Main.ADDRESS_ARTIFACT_ID, ADDRESS_SCHEMA);
        registry.register(
                "default",
                Main.ARTIFACT_ID,
                PERSON_SCHEMA,
                List.of(new ApicurioRegistry.Reference(
                        Main.ADDRESS_REF, "default", Main.ADDRESS_ARTIFACT_ID, "1")));

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
                Person person = Person.builder()
                        .name(FAKER.name().fullName())
                        .address(Address.builder()
                                .street(FAKER.address().streetAddress())
                                .city(FAKER.address().city())
                                .build())
                        .build();
                JsonNode value = MAPPER.valueToTree(Map.of(Main.VARIABLE_PERSON, person));
                producer.send(new ProducerRecord<>(Main.TOPIC, null, null, value));
                System.out.println("Produced: " + value);
            }
            producer.flush();
        }
    }
}
