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
 * Registers the JSON Schema in Apicurio Registry and produces records serialized with the Apicurio
 * JSON Schema serializer. The serializer is configured with
 * {@code apicurio.registry.headers.enabled=true}, so the schema coordinates travel in the Kafka
 * record headers instead of the message payload; the connector's converter is configured the same
 * way to read them back. Run with {@code -DmainClass=io.littlehorse.example.Producer}.
 */
public class Producer {

    private static final String BOOTSTRAP_SERVERS = "localhost:19092";
    private static final String APICURIO_URL = "http://localhost:8080/apis/registry/v3";
    private static final String TOPIC = "example-wfrun-apicurio-json-schema-headers";
    private static final String ARTIFACT_ID = TOPIC + "-value";

    private static final String SCHEMA = """
            {
              "$schema": "http://json-schema.org/draft-07/schema#",
              "title": "Force wielder envelope",
              "type": "object",
              "properties": {
                "wielder": {
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" },
                    "type": { "type": "string", "enum": ["SITH", "JEDI"] },
                    "lightsaberColor": { "type": "string" }
                  },
                  "required": ["name", "type", "lightsaberColor"]
                }
              },
              "required": ["wielder"]
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
        // Carry the schema coordinates in the Kafka record headers rather than the payload.
        config.put("apicurio.registry.headers.enabled", "true");

        try (KafkaProducer<String, JsonNode> producer = new KafkaProducer<>(config)) {
            for (int i = 0; i < datasetSize; i++) {
                SampleData.StarWars.ForceWielder wielder = SampleData.starWars().forceWielder();
                JsonNode value = MAPPER.valueToTree(Map.of(Main.VARIABLE_WIELDER, wielder));
                producer.send(new ProducerRecord<>(TOPIC, null, null, value));
                System.out.println("Produced: " + value);
            }
            producer.flush();
        }
    }
}
