package io.littlehorse.connect.transform;

import org.apache.kafka.connect.connector.ConnectRecord;

import java.util.List;

/**
 * Builds an operating domain (the record key, value, or headers) from constant literal values.
 * Mapping values are parsed as literals: {@code true}/{@code false} become booleans,
 * {@code null} becomes a null value, whole numbers become ints (or longs on overflow),
 * decimals become doubles, and anything else is a string. Wrap a value in double quotes to
 * force a string (e.g. {@code "42"}, {@code "true"}, or {@code "null"}). This transform does
 * not evaluate JSONPath; use {@link JsonPathMapperTransform} to derive values from the record.
 */
public abstract class LiteralMapperTransform<R extends ConnectRecord<R>>
        extends AbstractMapperTransform<R> {

    @Override
    protected Object createContext(R record) {
        // Literals are constant; they do not read from the record.
        return null;
    }

    @Override
    protected Mapping compile(List<String> targetPath, String value) {
        return new LiteralMapping(targetPath, parseLiteral(value.strip()));
    }

    private static Object parseLiteral(String literal) {
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
        protected R newRecord(R record, Object updatedValue) {
            return asKey(record, updatedValue);
        }
    }

    public static final class Value<R extends ConnectRecord<R>> extends LiteralMapperTransform<R> {
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
        protected R newRecord(R record, Object updatedValue) {
            return asHeaders(record, updatedValue);
        }
    }
}
