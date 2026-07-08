package io.littlehorse.connect.converter.apicurio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.apicurio.registry.serde.jsonschema.JsonSchemaKafkaDeserializer;
import io.apicurio.registry.serde.jsonschema.JsonSchemaKafkaSerializer;
import io.littlehorse.connect.LHSinkConnectorConfig;
import io.littlehorse.connect.util.VersionReader;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.connect.components.Versioned;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.errors.RetriableException;
import org.apache.kafka.connect.json.JsonConverter;
import org.apache.kafka.connect.storage.Converter;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A Kafka Connect {@link Converter} backed by Apicurio Registry's JSON Schema serde.
 *
 * <p>Unlike the converters shipped with Apicurio Registry, this implementation delegates the wire
 * (de)serialization to {@link JsonSchemaKafkaSerializer} and {@link JsonSchemaKafkaDeserializer} so
 * it benefits from the standard JSON Schema serde behaviour (schema resolution, optional payload
 * validation, message-type headers, references, ...). The bridge between the Apicurio serde (which
 * works with JSON) and the Kafka Connect data API is delegated to the built-in
 * {@link JsonConverter} (with {@code schemas.enable=false}), so the converter produces the same
 * schemaless Connect values a standard JSON converter would and therefore works with any sink or
 * source connector, not just the LittleHorse ones.
 *
 * <p>Every configuration property is forwarded verbatim to the underlying serde, so all
 * {@code apicurio.registry.*} settings (e.g. {@code apicurio.registry.url}) are honoured.
 */
public class JsonSchemaKafkaConverter implements Converter, Versioned {

    public static final String APICURIO_REGISTRY_URL_KEY = "apicurio.registry.url";
    public static final String APICURIO_AUTO_REGISTER_KEY = "apicurio.registry.auto-register";
    public static final String APICURIO_FIND_LATEST_KEY = "apicurio.registry.find-latest";

    // Documents the most common options for the CONFIGURATIONS.md reference. The converter forwards
    // every property to the Apicurio serde, so this definition is not used to parse or restrict the
    // configuration; any other apicurio.registry.* property is still honoured.
    public static final ConfigDef CONFIG_DEF = new ConfigDef()
            .define(
                    APICURIO_REGISTRY_URL_KEY,
                    Type.STRING,
                    Importance.HIGH,
                    "URL of the Apicurio Registry v3 API used to resolve JSON Schemas (e.g."
                            + " http://apicurio:8080/apis/registry/v3). When set on a connector,"
                            + " prefix it with key.converter. or value.converter.. Every other"
                            + " apicurio.registry.* property is forwarded to the underlying Apicurio"
                            + " JSON Schema serde; see the full list of serde properties at"
                            + " https://www.apicur.io/registry/docs/apicurio-registry/3.3.x/getting-started/assembly-configuring-kafka-client-serdes.html.")
            .define(
                    APICURIO_AUTO_REGISTER_KEY,
                    Type.BOOLEAN,
                    false,
                    Importance.MEDIUM,
                    "Whether to register the schema automatically when it is not already present in"
                            + " the registry. Typically only relevant when the converter serializes"
                            + " data (source connectors).")
            .define(
                    APICURIO_FIND_LATEST_KEY,
                    Type.BOOLEAN,
                    false,
                    Importance.LOW,
                    "Whether to use the latest version of the artifact when resolving a schema by"
                            + " its coordinates.")
            .define(
                    LHSinkConnectorConfig.TRANSIENT_ERRORS_TOLERANCE_KEY,
                    Type.STRING,
                    LHSinkConnectorConfig.TRANSIENT_ERRORS_TOLERANCE_TRANSIENTS,
                    ConfigDef.ValidString.in(
                            LHSinkConnectorConfig.TRANSIENT_ERRORS_TOLERANCE_NONE,
                            LHSinkConnectorConfig.TRANSIENT_ERRORS_TOLERANCE_TRANSIENTS),
                    Importance.MEDIUM,
                    "How to handle transient (retriable) errors such as the Apicurio Registry being"
                            + " temporarily unavailable. When 'transients' (default) the error is"
                            + " rethrown as a RetriableException, so Kafka Connect retries it for up"
                            + " to errors.retry.timeout before errors.tolerance applies; when 'none'"
                            + " the transient error is treated like any other conversion error and"
                            + " handled immediately according to errors.tolerance.");

    private final ObjectMapper mapper = new ObjectMapper();
    private final JsonSchemaKafkaSerializer<Object> serializer;
    private final JsonSchemaKafkaDeserializer<Object> deserializer;
    private final JsonConverter jsonConverter;
    private boolean tolerateTransientErrors = true;

    public JsonSchemaKafkaConverter() {
        this(
                new JsonSchemaKafkaSerializer<>(),
                new JsonSchemaKafkaDeserializer<>(),
                new JsonConverter());
    }

    JsonSchemaKafkaConverter(
            JsonSchemaKafkaSerializer<Object> serializer,
            JsonSchemaKafkaDeserializer<Object> deserializer,
            JsonConverter jsonConverter) {
        this.serializer = serializer;
        this.deserializer = deserializer;
        this.jsonConverter = jsonConverter;
    }

    @Override
    public String version() {
        return VersionReader.version();
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        serializer.configure(configs, isKey);
        deserializer.configure(configs, isKey);

        Object transientTolerance =
                configs.get(LHSinkConnectorConfig.TRANSIENT_ERRORS_TOLERANCE_KEY);
        tolerateTransientErrors = transientTolerance == null
                || LHSinkConnectorConfig.TRANSIENT_ERRORS_TOLERANCE_TRANSIENTS.equals(
                        transientTolerance.toString());

        // The Apicurio serde only deals with JSON. The built-in JsonConverter (schemaless) is used
        // to map between the Kafka Connect data API and plain JSON in both directions.
        Map<String, Object> jsonConverterConfig = new HashMap<>(configs);
        jsonConverterConfig.put("schemas.enable", false);
        jsonConverter.configure(jsonConverterConfig, isKey);
    }

    @Override
    public byte[] fromConnectData(String topic, Schema schema, Object value) {
        return fromConnectData(topic, null, schema, value);
    }

    @Override
    public byte[] fromConnectData(String topic, Headers headers, Schema schema, Object value) {
        if (value == null) {
            return null;
        }
        try {
            byte[] json = jsonConverter.fromConnectData(topic, schema, value);
            JsonNode node = mapper.readTree(json);
            return headers == null
                    ? serializer.serialize(topic, node)
                    : serializer.serialize(topic, headers, node);
        } catch (Exception e) {
            if (tolerateTransientErrors && isTransient(e)) {
                throw new RetriableException(
                        "Transient error serializing JSON Schema data for topic " + topic, e);
            }
            throw new DataException("Failed to serialize JSON Schema data for topic " + topic, e);
        }
    }

    @Override
    public SchemaAndValue toConnectData(String topic, byte[] value) {
        return toConnectData(topic, null, value);
    }

    @Override
    public SchemaAndValue toConnectData(String topic, Headers headers, byte[] value) {
        if (value == null) {
            return SchemaAndValue.NULL;
        }
        try {
            Object deserialized = headers == null
                    ? deserializer.deserialize(topic, value)
                    : deserializer.deserialize(topic, headers, value);
            byte[] json = mapper.writeValueAsBytes(deserialized);
            return jsonConverter.toConnectData(topic, json);
        } catch (Exception e) {
            if (tolerateTransientErrors && isTransient(e)) {
                throw new RetriableException(
                        "Transient error deserializing JSON Schema data for topic " + topic, e);
            }
            throw new DataException("Failed to deserialize JSON Schema data for topic " + topic, e);
        }
    }

    // HTTP status codes that indicate a transient, retriable server-side condition. 500 is
    // deliberately excluded: it is ambiguous (often a permanent server bug) and fromConnectData may
    // register schemas, so it should not silently burn the whole errors.retry.timeout window.
    private static final Set<Integer> RETRYABLE_HTTP_STATUS_CODES = Set.of(
            408, // Request Timeout
            429, // Too Many Requests
            502, // Bad Gateway
            503, // Service Unavailable
            504); // Gateway Timeout

    // Classifies a failure as a transient network error (retriable) vs. a permanent data error,
    // mirroring the Apicurio serde's own retry classification plus the JDK client's timeout type.
    // When the registry actually responds, the Apicurio SDK surfaces a Kiota ApiException (a
    // transitive Apicurio dependency), whose HTTP status decides. UnknownHostException is excluded
    // (like the serde) as it is usually a misconfigured host that should fail fast.
    private static boolean isTransient(Throwable error) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable cause = error; cause != null && seen.add(cause); cause = cause.getCause()) {
            // When the registry returns a response, its HTTP status is authoritative.
            if (cause instanceof com.microsoft.kiota.ApiException apiException) {
                int status = apiException.getResponseStatusCode();
                if (status > 0) {
                    return RETRYABLE_HTTP_STATUS_CODES.contains(status);
                }
            }
            if (cause instanceof java.net.ConnectException
                    || cause instanceof java.net.SocketTimeoutException
                    || cause instanceof java.net.http.HttpTimeoutException) {
                return true;
            }
            if (cause instanceof java.io.IOException && hasRetryableMessage(cause.getMessage())) {
                return true;
            }
            // The default Vert.x client closes the connection when the registry drops mid-request.
            if ("io.vertx.core.http.HttpClosedException".equals(cause.getClass().getName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasRetryableMessage(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.contains("connection reset")
                || normalized.contains("connection closed")
                || normalized.contains("broken pipe")
                || normalized.contains("stream was reset");
    }
}
