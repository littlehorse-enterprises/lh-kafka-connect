package e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import e2e.configs.E2ETest;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * End-to-end tests for {@code LiteralMapperTransform} that do not involve LittleHorse. A
 * {@code FileStreamSinkConnector} consumes the produced records, applies the transform (which
 * injects constant values, ignoring the input), and writes the resulting value to a file in
 * the container, which we read back to assert the result.
 */
public class TransformLiteralWithFileSinkTest extends E2ETest {

    private static final String TRANSFORM_VALUE =
            "io.littlehorse.connect.transform.LiteralMapperTransform$Value";

    private Map<String, Object> fileSinkConfig(String topic, String file) {
        Map<String, Object> config = new HashMap<>();
        config.put("tasks.max", 1);
        config.put("connector.class", "org.apache.kafka.connect.file.FileStreamSinkConnector");
        config.put("topics", topic);
        config.put("file", file);
        config.put("key.converter", "org.apache.kafka.connect.storage.StringConverter");
        config.put("value.converter", "org.apache.kafka.connect.json.JsonConverter");
        config.put("value.converter.schemas.enable", false);
        config.put("transforms", "literal");
        config.put("transforms.literal.type", TRANSFORM_VALUE);
        return config;
    }

    @Test
    public void shouldMapValueToNull() {
        String topic = "literal-value-to-null";
        String file = "/tmp/literal-value-to-null.txt";

        createTopics(topic);
        produceValues(topic, KafkaMessage.of("{\"ignored\":true}"));

        Map<String, Object> config = fileSinkConfig(topic, file);
        // An unquoted "null" becomes a real null value.
        config.put("transforms.literal.mapping.notes", "null");
        registerConnector(topic, config);

        await(() -> {
            String content = readFileFromKafkaConnect(file);
            assertThat(content).contains("notes=null");
        });
    }

    @Test
    public void shouldInjectLiteralTypes() {
        String topic = "literal-types";
        String file = "/tmp/literal-types.txt";

        createTopics(topic);
        produceValues(topic, KafkaMessage.of("{\"ignored\":true}"));

        Map<String, Object> config = fileSinkConfig(topic, file);
        config.put("transforms.literal.mapping.source", "kafka-connect");
        config.put("transforms.literal.mapping.priority", "5");
        config.put("transforms.literal.mapping.ratio", "3.14");
        config.put("transforms.literal.mapping.express", "true");
        config.put("transforms.literal.mapping.code", "\"42\"");
        registerConnector(topic, config);

        await(() -> {
            String content = readFileFromKafkaConnect(file);
            assertThat(content).contains("source=kafka-connect");
            assertThat(content).contains("priority=5");
            assertThat(content).contains("ratio=3.14");
            assertThat(content).contains("express=true");
            assertThat(content).contains("code=42");
        });
    }

    @Test
    public void shouldNullifyTheWholeValue() {
        String topic = "literal-whole-value-null";
        String file = "/tmp/literal-whole-value-null.txt";

        createTopics(topic);
        produceValues(topic, KafkaMessage.of("{\"keep\":\"me\"}"));

        Map<String, Object> config = fileSinkConfig(topic, file);
        // A bare mapping targets the whole value; the "null" literal replaces it with null.
        config.put("transforms.literal.mapping", "null");
        registerConnector(topic, config);

        await(() -> {
            String content = readFileFromKafkaConnect(file);
            assertThat(content).contains("null");
            assertThat(content).doesNotContain("keep");
        });
    }

    @Test
    public void shouldBuildNestedObjectFromLiterals() {
        String topic = "literal-nested";
        String file = "/tmp/literal-nested.txt";

        createTopics(topic);
        produceValues(topic, KafkaMessage.of("{\"ignored\":true}"));

        Map<String, Object> config = fileSinkConfig(topic, file);
        config.put("transforms.literal.mapping.meta.region", "us-east-1");
        config.put("transforms.literal.mapping.meta.tier", "gold");
        registerConnector(topic, config);

        await(() -> {
            String content = readFileFromKafkaConnect(file);
            assertThat(content).contains("region=us-east-1");
            assertThat(content).contains("tier=gold");
        });
    }
}
