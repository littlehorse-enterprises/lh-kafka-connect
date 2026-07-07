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
 * Registers the JSON Schema in Apicurio Registry and produces records to the topic using the
 * Apicurio JSON Schema serializer. Run with {@code -DmainClass=io.littlehorse.example.Producer}.
 */
public class Producer {

    private static final String BOOTSTRAP_SERVERS = "localhost:19092";
    private static final String APICURIO_URL = "http://localhost:8080/apis/registry/v3";
    private static final String TOPIC = "example-wfrun-apicurio-json-schema";
    private static final String ARTIFACT_ID = TOPIC + "-value";

    private static final String SCHEMA = """
            {
              "$schema": "http://json-schema.org/draft-07/schema#",
              "title": "Person envelope",
              "type": "object",
              "properties": {
                "person": {
                  "type": "object",
                  "properties": {
                    "firstName": { "type": "string" },
                    "lastName": { "type": "string" }
                  },
                  "required": ["firstName", "lastName"]
                }
              },
              "required": ["person"]
            }
            """;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) {
        int datasetSize = args.length > 0 ? Integer.parseInt(args[0]) : 10;

        new ApicurioRegistry(APICURIO_URL).register("default", ARTIFACT_ID, SCHEMA);

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
                SampleData.CharacterName name = SampleData.characterName();
                Person person = Person.builder()
                        .firstName(name.firstName())
                        .lastName(name.lastName())
                        .build();
                JsonNode value = MAPPER.valueToTree(Map.of(Main.VARIABLE_PERSON, person));
                producer.send(new ProducerRecord<>(TOPIC, null, null, value));
                System.out.println("Produced: " + value);
            }
            producer.flush();
        }
    }
}
