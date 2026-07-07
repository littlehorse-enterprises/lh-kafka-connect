package io.littlehorse.connect.converter.apicurio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.apicurio.registry.serde.jsonschema.JsonSchemaKafkaDeserializer;
import io.apicurio.registry.serde.jsonschema.JsonSchemaKafkaSerializer;

import org.apache.kafka.connect.data.SchemaAndValue;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.errors.RetriableException;
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

    // A record that does not match a valid schema fails deserialization. The converter must surface
    // a non-retriable DataException so Kafka Connect routes it to the DLQ (errors.tolerance=all)
    // instead of retrying it forever.
    @Test
    void shouldThrowNonRetriableDataExceptionWhenDeserializationFails() {
        when(deserializer.deserialize(eq("topic"), any(byte[].class)))
                .thenThrow(new RuntimeException("invalid schema"));

        assertThatThrownBy(() -> converter.toConnectData("topic", new byte[] {1}))
                .isInstanceOf(DataException.class)
                .isNotInstanceOf(RetriableException.class);
    }

    @Test
    void shouldThrowNonRetriableDataExceptionWhenSerializationFails() {
        when(serializer.serialize(eq("topic"), any()))
                .thenThrow(new RuntimeException("invalid schema"));

        assertThatThrownBy(
                        () -> converter.fromConnectData("topic", null, Map.of("name", "Obi-Wan")))
                .isInstanceOf(DataException.class)
                .isNotInstanceOf(RetriableException.class);
    }

    // A temporarily unavailable registry (network error) is transient, so the converter must throw
    // a RetriableException. Kafka Connect then retries the batch instead of routing the record to
    // the DLQ (RetriableExceptions are never sent to the DLQ).
    @Test
    void shouldThrowRetriableExceptionWhenRegistryIsUnavailableOnDeserialize() {
        when(deserializer.deserialize(eq("topic"), any(byte[].class)))
                .thenThrow(
                        new RuntimeException(new java.net.ConnectException("Connection refused")));

        assertThatThrownBy(() -> converter.toConnectData("topic", new byte[] {1}))
                .isInstanceOf(RetriableException.class);
    }

    @Test
    void shouldThrowRetriableExceptionWhenRegistryIsUnavailableOnSerialize() {
        when(serializer.serialize(eq("topic"), any()))
                .thenThrow(new RuntimeException(new java.net.SocketTimeoutException("timeout")));

        assertThatThrownBy(
                        () -> converter.fromConnectData("topic", null, Map.of("name", "Obi-Wan")))
                .isInstanceOf(RetriableException.class);
    }

    @Test
    void shouldTreatConnectionResetAsRetriable() {
        when(deserializer.deserialize(eq("topic"), any(byte[].class)))
                .thenThrow(
                        new RuntimeException(new java.io.IOException("Connection reset by peer")));

        assertThatThrownBy(() -> converter.toConnectData("topic", new byte[] {1}))
                .isInstanceOf(RetriableException.class);
    }

    // UnknownHostException is ambiguous (a transient DNS blip or a misconfigured host), so it is
    // treated as a permanent error to avoid retrying a bad configuration indefinitely; this matches
    // the Apicurio serde's own retry classifier, which excludes it.
    @Test
    void shouldTreatUnknownHostAsPermanent() {
        when(deserializer.deserialize(eq("topic"), any(byte[].class)))
                .thenThrow(new RuntimeException(new java.net.UnknownHostException("bad-host")));

        assertThatThrownBy(() -> converter.toConnectData("topic", new byte[] {1}))
                .isInstanceOf(DataException.class)
                .isNotInstanceOf(RetriableException.class);
    }

    // With transient.errors.tolerance=none, transient errors are handled like any other conversion
    // error (permanent DataException) rather than being retried.
    @Test
    void shouldTreatTransientErrorAsPermanentWhenToleranceIsNone() {
        converter.configure(
                Map.of(
                        "apicurio.registry.url",
                        "http://localhost:8080/apis/registry/v3",
                        "transient.errors.tolerance",
                        "none"),
                false);
        when(deserializer.deserialize(eq("topic"), any(byte[].class)))
                .thenThrow(
                        new RuntimeException(new java.net.ConnectException("Connection refused")));

        assertThatThrownBy(() -> converter.toConnectData("topic", new byte[] {1}))
                .isInstanceOf(DataException.class)
                .isNotInstanceOf(RetriableException.class);
    }

    @Test
    void shouldTreatRetryableHttpStatusAsRetriable() {
        when(deserializer.deserialize(eq("topic"), any(byte[].class)))
                .thenThrow(apiExceptionWithStatus(503));

        assertThatThrownBy(() -> converter.toConnectData("topic", new byte[] {1}))
                .isInstanceOf(RetriableException.class);
    }

    @Test
    void shouldTreatNonRetryableHttpStatusAsPermanent() {
        when(deserializer.deserialize(eq("topic"), any(byte[].class)))
                .thenThrow(apiExceptionWithStatus(409));

        assertThatThrownBy(() -> converter.toConnectData("topic", new byte[] {1}))
                .isInstanceOf(DataException.class)
                .isNotInstanceOf(RetriableException.class);
    }

    private static com.microsoft.kiota.ApiException apiExceptionWithStatus(int statusCode) {
        return new com.microsoft.kiota.ApiException() {
            @Override
            public int getResponseStatusCode() {
                return statusCode;
            }
        };
    }
}
