package io.littlehorse.connect.transform;

import static io.littlehorse.connect.transform.MapperTransformConfig.MAPPING_KEY;
import static io.littlehorse.connect.transform.MapperTransformConfig.MAPPING_PREFIX;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.header.Header;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class JsonPathMapperTransformTest {

    private static final String TOPIC = "my-topic";

    @Test
    @SuppressWarnings("unchecked")
    void shouldBuildValueOnlyFromMappings() {
        Map<String, Object> user = new HashMap<>();
        user.put("id", "u1");
        Map<String, Object> value = new HashMap<>();
        value.put("user", user);
        value.put("secret", "do-not-leak");

        SinkRecord record = new SinkRecord(TOPIC, 0, null, null, null, value, 0);

        JsonPathMapperTransform.Value<SinkRecord> mapper = new JsonPathMapperTransform.Value<>();
        mapper.configure(Map.of(MAPPING_PREFIX + "copied", "$.value.user.id"));

        SinkRecord result = mapper.apply(record);

        // A mapped domain is built only from its mappings: unmapped fields are dropped.
        Map<String, Object> resultValue = (Map<String, Object>) result.value();
        assertThat(resultValue).containsOnlyKeys("copied");
        assertThat(resultValue.get("copied")).isEqualTo("u1");
        assertThat(result.valueSchema()).isNull();

        mapper.close();
    }

    @Test
    void shouldReadFromStructAndPreserveValueWhenMappingKey() {
        Schema schema =
                SchemaBuilder.struct().field("name", Schema.STRING_SCHEMA).build();
        Struct struct = new Struct(schema).put("name", "Alice");

        SinkRecord record = new SinkRecord(TOPIC, 0, null, null, schema, struct, 0);

        JsonPathMapperTransform.Key<SinkRecord> mapper = new JsonPathMapperTransform.Key<>();
        mapper.configure(Map.of(MAPPING_KEY, "$.value.name"));

        SinkRecord result = mapper.apply(record);

        assertThat(result.key()).isEqualTo("Alice");
        assertThat(result.keySchema()).isNull();
        // The value domain is untouched, so its schema is preserved.
        assertThat(result.valueSchema()).isEqualTo(schema);
        assertThat(result.value()).isEqualTo(struct);

        mapper.close();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldMapKeyFromValue() {
        Map<String, Object> value = new HashMap<>();
        value.put("customerId", "c-42");

        SinkRecord record = new SinkRecord(TOPIC, 0, null, null, null, value, 0);

        JsonPathMapperTransform.Key<SinkRecord> mapper = new JsonPathMapperTransform.Key<>();
        mapper.configure(Map.of(MAPPING_KEY, "$.value.customerId"));

        SinkRecord result = mapper.apply(record);

        assertThat(result.key()).isEqualTo("c-42");
        assertThat(result.keySchema()).isNull();
        assertThat(((Map<String, Object>) result.value()).get("customerId")).isEqualTo("c-42");

        mapper.close();
    }

    @Test
    void shouldBuildHeadersFromMappings() {
        Map<String, Object> value = new HashMap<>();
        value.put("id", "ord-789");

        SinkRecord record = new SinkRecord(TOPIC, 0, null, null, null, value, 0);

        JsonPathMapperTransform.Headers<SinkRecord> mapper =
                new JsonPathMapperTransform.Headers<>();
        mapper.configure(Map.of(MAPPING_PREFIX + "orderId", "$.value.id"));

        SinkRecord result = mapper.apply(record);

        // Key and value domains are untouched; only headers are rebuilt.
        assertThat(result.value()).isEqualTo(value);
        Header header = result.headers().lastWithName("orderId");
        assertThat(header).isNotNull();
        assertThat(header.value()).isEqualTo("ord-789");

        mapper.close();
    }

    @Test
    void shouldBuildFlatDottedHeaderNames() {
        SinkRecord record = new SinkRecord(TOPIC, 0, null, null, null, null, 0);
        record.headers().addString("x-source", "checkout-svc");
        record.headers().addString("x-request-id", "req-abc");

        JsonPathMapperTransform.Headers<SinkRecord> mapper =
                new JsonPathMapperTransform.Headers<>();
        mapper.configure(Map.of(
                MAPPING_PREFIX + "trace.source", "$.headers.x-source",
                MAPPING_PREFIX + "trace.original", "$.headers.x-request-id"));

        SinkRecord result = mapper.apply(record);

        // Dotted targets become flat header names, not a nested "trace" object.
        assertThat(result.headers().lastWithName("trace")).isNull();
        assertThat(result.headers().lastWithName("trace.source").value()).isEqualTo("checkout-svc");
        assertThat(result.headers().lastWithName("trace.original").value()).isEqualTo("req-abc");

        mapper.close();
    }

    @Test
    void shouldReadFromExistingHeaders() {
        SinkRecord record = new SinkRecord(TOPIC, 0, null, null, null, null, 0);
        record.headers().addString("source", "upstream");

        JsonPathMapperTransform.Key<SinkRecord> mapper = new JsonPathMapperTransform.Key<>();
        mapper.configure(Map.of(MAPPING_KEY, "$.headers.source"));

        SinkRecord result = mapper.apply(record);

        assertThat(result.key()).isEqualTo("upstream");
        assertThat(result.keySchema()).isNull();

        mapper.close();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldSupportSumAggregation() {
        Map<String, Object> value = new HashMap<>();
        value.put("prices", List.of(10, 20, 30));

        SinkRecord record = new SinkRecord(TOPIC, 0, null, null, null, value, 0);

        JsonPathMapperTransform.Value<SinkRecord> mapper = new JsonPathMapperTransform.Value<>();
        mapper.configure(Map.of(MAPPING_PREFIX + "total", "$.value.prices.sum()"));

        SinkRecord result = mapper.apply(record);

        // Aggregation functions return a Double.
        assertThat(((Map<String, Object>) result.value()).get("total")).isEqualTo(60.0);

        mapper.close();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldSumTwoFields() {
        Map<String, Object> value = new HashMap<>();
        value.put("a", 10);
        value.put("b", 20);

        SinkRecord record = new SinkRecord(TOPIC, 0, null, null, null, value, 0);

        JsonPathMapperTransform.Value<SinkRecord> mapper = new JsonPathMapperTransform.Value<>();
        // Pass each field as a parameter; the function base path is the root.
        mapper.configure(Map.of(MAPPING_PREFIX + "total", "$.sum($.value.a, $.value.b)"));

        SinkRecord result = mapper.apply(record);

        assertThat(((Map<String, Object>) result.value()).get("total")).isEqualTo(30.0);

        mapper.close();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldPreserveStringTypeWhenJsonPathReadsNumericString() {
        Map<String, Object> value = new HashMap<>();
        // The input field is a string that happens to contain a number.
        value.put("code", "42");

        SinkRecord record = new SinkRecord(TOPIC, 0, null, null, null, value, 0);

        JsonPathMapperTransform.Value<SinkRecord> mapper = new JsonPathMapperTransform.Value<>();
        mapper.configure(Map.of(MAPPING_PREFIX + "code", "$.value.code"));

        SinkRecord result = mapper.apply(record);

        // JSONPath reads the value as-is; no type coercion happens, so it stays a String.
        Object code = ((Map<String, Object>) result.value()).get("code");
        assertThat(code).isInstanceOf(String.class).isEqualTo("42");

        mapper.close();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldPreserveIntegerTypeWhenJsonPathReadsInteger() {
        Map<String, Object> value = new HashMap<>();
        // The input field is an integer.
        value.put("count", 42);

        SinkRecord record = new SinkRecord(TOPIC, 0, null, null, null, value, 0);

        JsonPathMapperTransform.Value<SinkRecord> mapper = new JsonPathMapperTransform.Value<>();
        mapper.configure(Map.of(MAPPING_PREFIX + "count", "$.value.count"));

        SinkRecord result = mapper.apply(record);

        // JSONPath reads the value as-is; no type coercion happens, so it stays an Integer.
        Object count = ((Map<String, Object>) result.value()).get("count");
        assertThat(count).isInstanceOf(Integer.class).isEqualTo(42);

        mapper.close();
    }

    @Test
    void shouldFailWhenDollarStringIsNotValidJsonPath() {
        JsonPathMapperTransform.Value<SinkRecord> mapper = new JsonPathMapperTransform.Value<>();
        // A value starting with '$' is compiled as a JSONPath. "$5.00" is invalid JSONPath
        // syntax, so configuration fails fast.
        assertThatThrownBy(() -> mapper.configure(Map.of(MAPPING_PREFIX + "price", "$5.00")))
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("Invalid JSONPath");

        mapper.close();
    }

    @Test
    void shouldFailWhenValueIsNotJsonPath() {
        JsonPathMapperTransform.Value<SinkRecord> mapper = new JsonPathMapperTransform.Value<>();
        // This transform is JSONPath-only: literals (values not starting with '$') are
        // rejected. Use the LiteralMapperTransform for constant values.
        assertThatThrownBy(() -> mapper.configure(Map.of(MAPPING_PREFIX + "source", "kafka")))
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("JSONPath expression");

        mapper.close();
    }

    @Test
    void shouldFailWhenValueIsNull() {
        JsonPathMapperTransform.Value<SinkRecord> mapper = new JsonPathMapperTransform.Value<>();
        // A bare null is not a JSONPath expression, so configuration fails fast.
        Map<String, String> config = new HashMap<>();
        config.put(MAPPING_PREFIX + "source", null);
        assertThatThrownBy(() -> mapper.configure(config))
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("JSONPath expression");

        mapper.close();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldSupportConcat() {
        Map<String, Object> value = new HashMap<>();
        value.put("name", "John");
        value.put("lastName", "Doe");

        SinkRecord record = new SinkRecord(TOPIC, 0, null, null, null, value, 0);

        JsonPathMapperTransform.Value<SinkRecord> mapper = new JsonPathMapperTransform.Value<>();
        // All pieces are passed as parameters; the function base path is the root.
        mapper.configure(Map.of(
                MAPPING_PREFIX + "fullName", "$.concat($.value.name, \" \", $.value.lastName)"));

        SinkRecord result = mapper.apply(record);

        assertThat(((Map<String, Object>) result.value()).get("fullName")).isEqualTo("John Doe");

        mapper.close();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldHandleNullValueWithoutNpe() {
        // A null value must not produce a poison pill: missing paths resolve to null.
        SinkRecord record = new SinkRecord(TOPIC, 0, null, "k-1", null, null, 0);

        JsonPathMapperTransform.Value<SinkRecord> mapper = new JsonPathMapperTransform.Value<>();
        mapper.configure(Map.of(MAPPING_PREFIX + "copied", "$.value.user.id"));

        SinkRecord result = mapper.apply(record);

        Map<String, Object> resultValue = (Map<String, Object>) result.value();
        assertThat(resultValue).containsOnlyKeys("copied");
        assertThat(resultValue.get("copied")).isNull();
        // Key is untouched.
        assertThat(result.key()).isEqualTo("k-1");

        mapper.close();
    }

    @Test
    void shouldHandleNullKeyAndValueWholeDomain() {
        SinkRecord record = new SinkRecord(TOPIC, 0, null, null, null, null, 0);

        JsonPathMapperTransform.Key<SinkRecord> mapper = new JsonPathMapperTransform.Key<>();
        mapper.configure(Map.of(MAPPING_KEY, "$.value.id"));

        assertThatNoException().isThrownBy(() -> {
            SinkRecord result = mapper.apply(record);
            assertThat(result.key()).isNull();
            assertThat(result.keySchema()).isNull();
        });

        mapper.close();
    }

    @Test
    void shouldAddNullHeaderValueWithoutNpe() {
        SinkRecord record = new SinkRecord(TOPIC, 0, null, null, null, null, 0);

        JsonPathMapperTransform.Headers<SinkRecord> mapper =
                new JsonPathMapperTransform.Headers<>();
        mapper.configure(Map.of(MAPPING_PREFIX + "orderId", "$.value.id"));

        SinkRecord result = mapper.apply(record);

        Header header = result.headers().lastWithName("orderId");
        assertThat(header).isNotNull();
        assertThat(header.value()).isNull();

        mapper.close();
    }
}
