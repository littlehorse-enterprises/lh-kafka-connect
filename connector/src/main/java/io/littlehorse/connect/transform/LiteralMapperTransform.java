package io.littlehorse.connect.transform;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.apache.kafka.connect.connector.ConnectRecord;

import java.util.List;

/**
 * Builds an operating domain (the record key, value, or headers) from constant literal values.
 * Mapping values are parsed as literals: {@code true}/{@code false} become booleans,
 * {@code null} becomes a null value, whole numbers become ints (or longs on overflow),
 * decimals become doubles, and anything else is a string. Wrap a value in double quotes to
 * force a string (e.g. {@code "42"}, {@code "true"}, or {@code "null"}). Set
 * {@code implicit.casting.enabled} to {@code false} to disable this inference and keep every
 * value as its original string. This transform does not evaluate JSONPath; use {@link
 * JsonPathMapperTransform} to derive values from the record.
 */
public abstract class LiteralMapperTransform<R extends ConnectRecord<R>>
        extends AbstractMapperTransform<R> {

    public static final String IMPLICIT_CASTING_CONFIG = "implicit.casting.enabled";

    public static final ConfigDef CONFIG_DEF = MapperTransformConfig.configDef(
                    "Defines a mapping written into the operating domain. Each mapping is its own property:"
                            + " the bare ``mapping`` targets the whole domain, while ``mapping.<path>`` (a"
                            + " dot-separated path) builds nested objects; for the ``$Headers`` variant the"
                            + " whole path is a single, flat header name. The value is a constant whose type"
                            + " is inferred (an integer, a double, ``true``/``false`` as a boolean,"
                            + " ``null`` as a null value, otherwise a string; wrap the value in double"
                            + " quotes to force a string). Use the ``$Key``, ``$Value`` or ``$Headers``"
                            + " nested variant to choose whether the record key, value or headers are"
                            + " written. The constants are merged onto the existing domain, overriding any"
                            + " fields with the same name.")
            .define(
                    IMPLICIT_CASTING_CONFIG,
                    Type.BOOLEAN,
                    true,
                    Importance.MEDIUM,
                    "When ``true`` (the default), each mapping value is parsed into an inferred type"
                            + " (an integer, a double, ``true``/``false`` as a boolean, ``null`` as a null"
                            + " value, otherwise a string). When ``false``, every value is kept as its"
                            + " original string with no type inference.");

    private boolean implicitCasting = true;

    @Override
    public ConfigDef config() {
        return CONFIG_DEF;
    }

    @Override
    protected void onConfigure(MapperTransformConfig config) {
        implicitCasting = config.getBoolean(IMPLICIT_CASTING_CONFIG);
    }

    @Override
    protected Object createContext(R record) {
        // Literals are constant; they do not read from the record.
        return null;
    }

    @Override
    protected Mapping compile(List<String> targetPath, String value) {
        // A bare null mapping value (without quotes) maps to a null value.
        return new LiteralMapping(targetPath, value == null ? null : parseLiteral(value.strip()));
    }

    private Object parseLiteral(String literal) {
        if (!implicitCasting) {
            return literal;
        }
        if (literal.length() >= 2 && literal.startsWith("\"") && literal.endsWith("\"")) {
            return literal.substring(1, literal.length() - 1);
        }
        if (literal.equalsIgnoreCase("null")) {
            return null;
        }
        if (literal.equalsIgnoreCase("true")) {
            return Boolean.TRUE;
        }
        if (literal.equalsIgnoreCase("false")) {
            return Boolean.FALSE;
        }
        if (literal.matches("[+-]?\\d+")) {
            try {
                return Integer.valueOf(literal);
            } catch (NumberFormatException overflow) {
                return Long.valueOf(literal);
            }
        }
        if (literal.matches("[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?")) {
            return Double.valueOf(literal);
        }
        return literal;
    }

    private static final class LiteralMapping extends Mapping {
        private final Object value;

        private LiteralMapping(List<String> targetPath, Object value) {
            super(targetPath);
            this.value = value;
        }

        @Override
        protected Object resolve(Object context) {
            return value;
        }
    }

    public static final class Key<R extends ConnectRecord<R>> extends LiteralMapperTransform<R> {
        @Override
        protected Object initialDomain(R record) {
            return record.key();
        }

        @Override
        protected R newRecord(R record, Object updatedValue) {
            return asKey(record, updatedValue);
        }
    }

    public static final class Value<R extends ConnectRecord<R>> extends LiteralMapperTransform<R> {
        @Override
        protected Object initialDomain(R record) {
            return record.value();
        }

        @Override
        protected R newRecord(R record, Object updatedValue) {
            return asValue(record, updatedValue);
        }
    }

    public static final class Headers<R extends ConnectRecord<R>>
            extends LiteralMapperTransform<R> {
        @Override
        protected List<String> parseTarget(String target) {
            return flatTarget(target);
        }

        @Override
        protected Object initialDomain(R record) {
            return headersToMap(record);
        }

        @Override
        protected R newRecord(R record, Object updatedValue) {
            return asHeaders(record, updatedValue);
        }
    }
}
