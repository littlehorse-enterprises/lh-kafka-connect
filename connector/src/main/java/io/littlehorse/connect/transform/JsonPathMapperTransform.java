package io.littlehorse.connect.transform;

import static io.littlehorse.connect.transform.MapperTransformConfig.MAPPING_PREFIX;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import io.littlehorse.connect.util.JsonPathEvaluator;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.connector.ConnectRecord;

import java.util.List;

/**
 * Builds an operating domain (the record key, value, or headers) by evaluating JSONPath
 * expressions against an envelope of the record: {@code {key, value, headers}}. Mapping values
 * must be JSONPath expressions (they start with {@code $}); use {@link LiteralMapperTransform}
 * to inject constant values.
 */
public abstract class JsonPathMapperTransform<R extends ConnectRecord<R>>
        extends AbstractMapperTransform<R> {

    public static final ConfigDef CONFIG_DEF = MapperTransformConfig.configDef(
            "Defines a mapping written into the operating domain. Each mapping is its own property:"
                    + " the bare ``mapping`` targets the whole domain, while ``mapping.<path>`` (a"
                    + " dot-separated path such as ``mapping.pilot.vehicle.model``) builds nested"
                    + " objects; for the ``$Headers`` variant the whole path is a single, flat"
                    + " header name. The value must be a JSONPath expression (starting with '$')"
                    + " evaluated against the record envelope ``{key, value, headers}``, and"
                    + " functions such as ``concat`` and ``sum`` are supported. Use the ``$Key``,"
                    + " ``$Value`` or ``$Headers`` nested variant to choose whether the record key,"
                    + " value or headers are rebuilt. The operating domain is built from scratch, so"
                    + " unmapped fields are dropped.");

    private final JsonPathEvaluator evaluator = new JsonPathEvaluator();

    @Override
    public ConfigDef config() {
        return CONFIG_DEF;
    }

    @Override
    protected Object createContext(R record) {
        return evaluator.parse(record);
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
