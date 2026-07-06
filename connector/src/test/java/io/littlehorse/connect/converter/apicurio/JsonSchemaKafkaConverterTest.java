package io.littlehorse.connect.converter.apicurio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.apicurio.registry.serde.jsonschema.JsonSchemaKafkaDeserializer;
import io.apicurio.registry.serde.jsonschema.JsonSchemaKafkaSerializer;

import org.apache.kafka.connect.data.SchemaAndValue;
import org.apache.kafka.connect.json.JsonConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

@ExtendWith(MockitoExtension.class)
class JsonSchemaKafkaConverterTest {

    @Mock
    private JsonSchemaKafkaSerializer<Object> serializer;

    @Mock
    private JsonSchemaKafkaDeserializer<Object> deserializer;

    private final ObjectMapper mapper = new ObjectMapper();
    private JsonSchemaKafkaConverter converter;

    @BeforeEach
    void setUp() {
        converter = new JsonSchemaKafkaConverter(serializer, deserializer, new JsonConverter());
        converter.configure(
                Map.of("apicurio.registry.url", "http://localhost:8080/apis/registry/v3"), false);
    }

    @Test
    void shouldConfigureUnderlyingSerde() {
        verify(serializer).configure(any(), eq(false));
        verify(deserializer).configure(any(), eq(false));
    }

    @Test
    void shouldSerializeConnectDataThroughTheSerdeAsJson() {
        byte[] expected = {1, 2, 3};
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        when(serializer.serialize(eq("topic"), captor.capture())).thenReturn(expected);

        byte[] result = converter.fromConnectData("topic", null, Map.of("name", "Obi-Wan"));

        assertThat(result).isEqualTo(expected);
        JsonNode captured = (JsonNode) captor.getValue();
        assertThat(captured.get("name").asText()).isEqualTo("Obi-Wan");
    }

    @Test
    void shouldDeserializeIntoSchemalessConnectData() {
        JsonNode node = mapper.createObjectNode().put("name", "Leia");
        when(deserializer.deserialize(eq("topic"), any(byte[].class))).thenReturn(node);

        SchemaAndValue result = converter.toConnectData("topic", new byte[] {1});

        assertThat(result.schema()).isNull();
        assertThat(result.value()).isEqualTo(Map.of("name", "Leia"));
    }

    @Test
    void shouldReturnNullWhenSerializingNullValue() {
        assertThat(converter.fromConnectData("topic", null, null)).isNull();
    }

    @Test
    void shouldReturnNullSchemaAndValueWhenDeserializingNullBytes() {
        assertThat(converter.toConnectData("topic", null)).isEqualTo(SchemaAndValue.NULL);
    }
}
