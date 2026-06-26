package io.littlehorse.connect.transform;

import static io.littlehorse.connect.transform.MapperTransformConfig.MAPPING_PREFIX;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

import io.littlehorse.connect.util.ObjectMapper;

import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.header.Header;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds an operating domain (the record key, value, or headers) by evaluating JSONPath
 * expressions against an envelope of the record: {@code {key, value, headers}}. Mapping values
 * must be JSONPath expressions (they start with {@code $}); use {@link LiteralMapperTransform}
 * to inject constant values.
 */
public abstract class JsonPathMapperTransform<R extends ConnectRecord<R>>
        extends AbstractMapperTransform<R> {

    private static final String KEY_DOMAIN = "key";
    private static final String VALUE_DOMAIN = "value";
    private static final String HEADERS_DOMAIN = "headers";

    private static final Configuration JSONPATH_CONFIG = Configuration.builder()
            .options(Option.SUPPRESS_EXCEPTIONS, Option.DEFAULT_PATH_LEAF_TO_NULL)
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected Object createContext(R record) {
        return JsonPath.using(JSONPATH_CONFIG).parse(buildEnvelope(record));
    }

    @Override
    protected Mapping compile(List<String> targetPath, String value) {
        // A JSONPath always starts with '$'; reject literals so responsibilities stay separate.
        if (!value.strip().startsWith("$")) {
            throw new ConfigException(
                    MAPPING_PREFIX + String.join(".", targetPath),
                    value,
                    "Value must be a JSONPath expression (starting with '$'). Use the"
                            + " LiteralMapperTransform to inject constant values.");
        }
        try {
            return new JsonPathMapping(targetPath, JsonPath.compile(value));
        } catch (RuntimeException e) {
            throw new ConfigException(
                    MAPPING_PREFIX + String.join(".", targetPath),
                    value,
                    "Invalid JSONPath: " + e.getMessage());
        }
    }

    private Map<String, Object> buildEnvelope(R record) {
        Map<String, Object> envelope = new HashMap<>();
        envelope.put(KEY_DOMAIN, objectMapper.removeStruct(record.key()));
        envelope.put(VALUE_DOMAIN, objectMapper.removeStruct(record.value()));
        envelope.put(HEADERS_DOMAIN, headersToMap(record.headers()));
        return envelope;
    }

    private Map<String, Object> headersToMap(Iterable<Header> headers) {
        Map<String, Object> result = new HashMap<>();
        for (Header header : headers) {
            result.put(header.key(), objectMapper.removeStruct(header.value()));
        }
        return result;
    }

    private static final class JsonPathMapping extends Mapping {
        private final JsonPath compiledPath;

        private JsonPathMapping(List<String> targetPath, JsonPath compiledPath) {
            super(targetPath);
            this.compiledPath = compiledPath;
        }

        @Override
        protected Object resolve(Object context) {
            return ((DocumentContext) context).read(compiledPath);
        }
    }

    public static final class Key<R extends ConnectRecord<R>> extends JsonPathMapperTransform<R> {
        @Override
        protected R newRecord(R record, Object updatedValue) {
            return asKey(record, updatedValue);
        }
    }

    public static final class Value<R extends ConnectRecord<R>> extends JsonPathMapperTransform<R> {
        @Override
        protected R newRecord(R record, Object updatedValue) {
            return asValue(record, updatedValue);
        }
    }

    public static final class Headers<R extends ConnectRecord<R>>
            extends JsonPathMapperTransform<R> {
        @Override
        protected List<String> parseTarget(String target) {
            return flatTarget(target);
        }

        @Override
        protected R newRecord(R record, Object updatedValue) {
            return asHeaders(record, updatedValue);
        }
    }
}
