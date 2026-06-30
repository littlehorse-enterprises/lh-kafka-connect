package e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import e2e.configs.E2ETest;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * End-to-end tests for {@code JsonPathMapperTransform} that do not involve LittleHorse. A
 * {@code FileStreamSinkConnector} consumes the produced records, applies the transform (the
 * value converter runs first, so the transform receives a parsed value), and writes the
 * resulting value to a file in the container, which we read back to assert the result.
 */
public class TransformJsonPathWithFileSinkTest extends E2ETest {

    private static final String TRANSFORM_VALUE =
            "io.littlehorse.connect.transform.JsonPathMapperTransform$Value";

    private Map<String, Object> fileSinkConfig(String topic, String file) {
        Map<String, Object> config = new HashMap<>();
        config.put("tasks.max", 1);
        config.put("connector.class", "org.apache.kafka.connect.file.FileStreamSinkConnector");
        config.put("topics", topic);
        config.put("file", file);
        config.put("key.converter", "org.apache.kafka.connect.json.JsonConverter");
        config.put("key.converter.schemas.enable", false);
        config.put("value.converter", "org.apache.kafka.connect.json.JsonConverter");
        config.put("value.converter.schemas.enable", false);
        config.put("transforms", "map");
        config.put("transforms.map.type", TRANSFORM_VALUE);
        return config;
    }

    @Test
    public void shouldMapJsonValueToSimpleStringValue() {
        String topic = "jsonpath-value-to-string";
        String file = "/tmp/jsonpath-value-to-string.txt";

        createTopics(topic);
        produceValues(
                topic, KafkaMessage.of("{\"user\":{\"id\":\"u1\"},\"secret\":\"do-not-leak\"}"));

        Map<String, Object> config = fileSinkConfig(topic, file);
        // A bare mapping replaces the whole value with the resolved scalar.
        config.put("transforms.map.mapping", "$.value.user.id");
        registerConnector(topic, config);

        await(() -> {
            String content = readFileFromKafkaConnect(file);
            assertThat(content).contains("u1");
            assertThat(content).doesNotContain("do-not-leak");
        });
    }

    @Test
    public void shouldBuildJsonValueFromKeyWhenValueIsNull() {
        String topic = "jsonpath-key-to-value";
        String file = "/tmp/jsonpath-key-to-value.txt";

        createTopics(topic);
        // JSON key, null value: the transform builds the value by reading from the key.
        produceValues(topic, KafkaMessage.of("{\"id\":\"abc-123\"}", null));

        Map<String, Object> config = fileSinkConfig(topic, file);
        config.put("transforms.map.mapping.id", "$.key.id");
        registerConnector(topic, config);

        await(() -> {
            String content = readFileFromKafkaConnect(file);
            assertThat(content).contains("id=abc-123");
        });
    }

    @Test
    public void shouldApplySumAndConcat() {
        String topic = "jsonpath-sum-concat";
        String file = "/tmp/jsonpath-sum-concat.txt";

        createTopics(topic);
        produceValues(
                topic,
                KafkaMessage.of("{\"firstName\":\"John\",\"lastName\":\"Doe\","
                        + "\"subtotal\":90,\"tax\":10}"));

        Map<String, Object> config = fileSinkConfig(topic, file);
        config.put(
                "transforms.map.mapping.fullName",
                "$.concat($.value.firstName, \" \", $.value.lastName)");
        config.put("transforms.map.mapping.total", "$.sum($.value.subtotal, $.value.tax)");
        registerConnector(topic, config);

        await(() -> {
            String content = readFileFromKafkaConnect(file);
            assertThat(content).contains("fullName=John Doe");
            assertThat(content).contains("total=100.0");
        });
    }

    @Test
    public void shouldBuildNestedObjectAndAggregateArray() {
        String topic = "jsonpath-nested-aggregate";
        String file = "/tmp/jsonpath-nested-aggregate.txt";

        createTopics(topic);
        produceValues(topic, KafkaMessage.of("{\"prices\":[10,20,30],\"region\":\"us-east-1\"}"));

        Map<String, Object> config = fileSinkConfig(topic, file);
        // Dotted targets build nested objects in the value.
        config.put("transforms.map.mapping.totals.sum", "$.value.prices.sum()");
        config.put("transforms.map.mapping.meta.region", "$.value.region");
        registerConnector(topic, config);

        await(() -> {
            String content = readFileFromKafkaConnect(file);
            assertThat(content).contains("sum=60.0");
            assertThat(content).contains("region=us-east-1");
        });
    }
}
