package io.littlehorse.connect.transform;

import static io.littlehorse.connect.transform.MapperTransformConfig.MAPPING_KEY;
import static io.littlehorse.connect.transform.MapperTransformConfig.MAPPING_PREFIX;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.kafka.connect.header.Header;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class LiteralMapperTransformTest {

    private static final String TOPIC = "my-topic";

    @Test
    @SuppressWarnings("unchecked")
    void shouldInferLiteralTypes() {
        SinkRecord record = new SinkRecord(TOPIC, 0, null, null, null, new HashMap<>(), 0);

        LiteralMapperTransform.Value<SinkRecord> mapper = new LiteralMapperTransform.Value<>();
        Map<String, String> config = new HashMap<>();
        config.put(MAPPING_PREFIX + "name", "my hard coded string");
        config.put(MAPPING_PREFIX + "count", "42");
        config.put(MAPPING_PREFIX + "ratio", "3.14");
        config.put(MAPPING_PREFIX + "enabled", "true");
        config.put(MAPPING_PREFIX + "disabled", "false");
        mapper.configure(config);

        SinkRecord result = mapper.apply(record);
        Map<String, Object> value = (Map<String, Object>) result.value();

        assertThat(value.get("name")).isEqualTo("my hard coded string");
        assertThat(value.get("count")).isInstanceOf(Integer.class).isEqualTo(42);
        assertThat(value.get("ratio")).isInstanceOf(Double.class).isEqualTo(3.14);
        assertThat(value.get("enabled")).isEqualTo(true);
        assertThat(value.get("disabled")).isEqualTo(false);

        mapper.close();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldSupportNullLiteral() {
        SinkRecord record = new SinkRecord(TOPIC, 0, null, null, null, new HashMap<>(), 0);

        LiteralMapperTransform.Value<SinkRecord> mapper = new LiteralMapperTransform.Value<>();
        mapper.configure(Map.of(MAPPING_PREFIX + "notes", "null"));

        SinkRecord result = mapper.apply(record);
        Map<String, Object> value = (Map<String, Object>) result.value();

        // An unquoted "null" becomes a real null value.
        assertThat(value).containsKey("notes");
        assertThat(value.get("notes")).isNull();

        mapper.close();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldForceStringWithDoubleQuotes() {
        SinkRecord record = new SinkRecord(TOPIC, 0, null, null, null, new HashMap<>(), 0);

        LiteralMapperTransform.Value<SinkRecord> mapper = new LiteralMapperTransform.Value<>();
        Map<String, String> config = new HashMap<>();
        // Double quotes force a string, even when the content looks like another type.
        config.put(MAPPING_PREFIX + "code", "\"42\"");
        config.put(MAPPING_PREFIX + "flag", "\"true\"");
        config.put(MAPPING_PREFIX + "blank", "\"null\"");
        mapper.configure(config);

        SinkRecord result = mapper.apply(record);
        Map<String, Object> value = (Map<String, Object>) result.value();

        assertThat(value.get("code")).isInstanceOf(String.class).isEqualTo("42");
        assertThat(value.get("flag")).isInstanceOf(String.class).isEqualTo("true");
        assertThat(value.get("blank")).isInstanceOf(String.class).isEqualTo("null");

        mapper.close();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldKeepDollarStringAsLiteral() {
        SinkRecord record = new SinkRecord(TOPIC, 0, null, null, null, new HashMap<>(), 0);

        LiteralMapperTransform.Value<SinkRecord> mapper = new LiteralMapperTransform.Value<>();
        // This transform never evaluates JSONPath, so a '$' value is just a string.
        mapper.configure(Map.of(MAPPING_PREFIX + "price", "$5.00"));

        SinkRecord result = mapper.apply(record);

        assertThat(((Map<String, Object>) result.value()).get("price")).isEqualTo("$5.00");

        mapper.close();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldBuildNestedObjects() {
        SinkRecord record = new SinkRecord(TOPIC, 0, null, null, null, new HashMap<>(), 0);

        LiteralMapperTransform.Value<SinkRecord> mapper = new LiteralMapperTransform.Value<>();
        mapper.configure(Map.of(MAPPING_PREFIX + "meta.region", "us-east-1"));

        SinkRecord result = mapper.apply(record);
        Map<String, Object> value = (Map<String, Object>) result.value();

        assertThat(value).containsOnlyKeys("meta");
        Map<String, Object> meta = (Map<String, Object>) value.get("meta");
        assertThat(meta.get("region")).isEqualTo("us-east-1");

        mapper.close();
    }

    @Test
    void shouldStampConstantHeadersLeavingKeyAndValueUntouched() {
        Map<String, Object> value = new HashMap<>();
        value.put("id", "ord-789");

        SinkRecord record = new SinkRecord(TOPIC, 0, null, "k-1", null, value, 0);

        LiteralMapperTransform.Headers<SinkRecord> mapper = new LiteralMapperTransform.Headers<>();
        Map<String, String> config = new HashMap<>();
        config.put(MAPPING_PREFIX + "processed-by", "lh-kafka-connect");
        config.put(MAPPING_PREFIX + "priority", "5");
        config.put(MAPPING_PREFIX + "trace-enabled", "true");
        config.put(MAPPING_PREFIX + "notes", "null");
        mapper.configure(config);

        SinkRecord result = mapper.apply(record);

        // Key and value are untouched; only headers are rebuilt.
        assertThat(result.key()).isEqualTo("k-1");
        assertThat(result.value()).isEqualTo(value);

        assertThat(result.headers().lastWithName("processed-by").value())
                .isEqualTo("lh-kafka-connect");
        assertThat(result.headers().lastWithName("priority").value()).isEqualTo(5);
        assertThat(result.headers().lastWithName("trace-enabled").value()).isEqualTo(true);
        Header notes = result.headers().lastWithName("notes");
        assertThat(notes).isNotNull();
        assertThat(notes.value()).isNull();

        mapper.close();
    }

    @Test
    void shouldSetKeyFromLiteral() {
        SinkRecord record = new SinkRecord(TOPIC, 0, null, null, null, null, 0);

        LiteralMapperTransform.Key<SinkRecord> mapper = new LiteralMapperTransform.Key<>();
        mapper.configure(Map.of(MAPPING_KEY, "static-key"));

        SinkRecord result = mapper.apply(record);

        assertThat(result.key()).isEqualTo("static-key");
        assertThat(result.keySchema()).isNull();

        mapper.close();
    }
}
