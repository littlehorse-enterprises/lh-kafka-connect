package io.littlehorse.connect.converter.apicurio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.apicurio.registry.serde.jsonschema.JsonSchemaKafkaDeserializer;
import io.apicurio.registry.serde.jsonschema.JsonSchemaKafkaSerializer;
import io.littlehorse.connect.util.VersionReader;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.connect.components.Versioned;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.json.JsonConverter;
import org.apache.kafka.connect.storage.Converter;

import java.util.HashMap;
import java.util.Map;

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
                            + " its coordinates.");

    private final ObjectMapper mapper = new ObjectMapper();
    private final JsonSchemaKafkaSerializer<Object> serializer;
    private final JsonSchemaKafkaDeserializer<Object> deserializer;
    private final JsonConverter jsonConverter;

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
            throw new DataException("Failed to deserialize JSON Schema data for topic " + topic, e);
        }
    }
}
