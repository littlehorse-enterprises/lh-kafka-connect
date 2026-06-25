package io.littlehorse.connect.transform;

import static io.littlehorse.connect.transform.JsonPathMapperConfig.MAPPINGS_KEY;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class JsonPathMapperTest {

    private static final String TOPIC = "my-topic";

    @Test
    @SuppressWarnings("unchecked")
    void shouldMapSchemalessValueIntoHeaderAndValue() {
        Map<String, Object> user = new HashMap<>();
        user.put("id", "u1");
        Map<String, Object> value = new HashMap<>();
        value.put("user", user);
        value.put("secret", "do-not-leak");

        SinkRecord record = new SinkRecord(TOPIC, 0, null, null, null, value, 0);

        JsonPathMapper<SinkRecord> mapper = new JsonPathMapper<>();
        mapper.configure(Map.of(
                MAPPINGS_KEY,
                "{\"header.userId\":\"$.value.user.id\",\"value.copied\":\"$.value.user.id\"}"));

        SinkRecord result = mapper.apply(record);

        assertThat(result.headers().lastWithName("userId").value()).isEqualTo("u1");

        // A mapped domain is built only from its mappings: unmapped fields are dropped.
        Map<String, Object> resultValue = (Map<String, Object>) result.value();
        assertThat(resultValue).containsOnlyKeys("copied");
        assertThat(resultValue.get("copied")).isEqualTo("u1");
        assertThat(result.valueSchema()).isNull();

        mapper.close();
    }

    @Test
    void shouldReadFromStructAndPreserveUntouchedSchema() {
        Schema schema =
                SchemaBuilder.struct().field("name", Schema.STRING_SCHEMA).build();
        Struct struct = new Struct(schema).put("name", "Alice");

        SinkRecord record = new SinkRecord(TOPIC, 0, null, null, schema, struct, 0);

        JsonPathMapper<SinkRecord> mapper = new JsonPathMapper<>();
        mapper.configure(Map.of(MAPPINGS_KEY, "{\"header.name\":\"$.value.name\"}"));

        SinkRecord result = mapper.apply(record);

        assertThat(result.headers().lastWithName("name").value()).isEqualTo("Alice");
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

        JsonPathMapper<SinkRecord> mapper = new JsonPathMapper<>();
        mapper.configure(Map.of(MAPPINGS_KEY, "{\"key\":\"$.value.customerId\"}"));

        SinkRecord result = mapper.apply(record);

        assertThat(result.key()).isEqualTo("c-42");
        assertThat(result.keySchema()).isNull();
        assertThat(((Map<String, Object>) result.value()).get("customerId")).isEqualTo("c-42");

        mapper.close();
    }
}
