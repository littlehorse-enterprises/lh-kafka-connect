package io.littlehorse.connect.predicate;

import static io.littlehorse.connect.predicate.FilterByFieldPredicateConfig.OPERATION_EXCLUDE;
import static io.littlehorse.connect.predicate.FilterByFieldPredicateConfig.OPERATION_INCLUDE;

import io.littlehorse.connect.util.VersionReader;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.components.Versioned;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.transforms.predicates.Predicate;

import java.util.Map;

public abstract class FilterByFieldPredicate<R extends ConnectRecord<R>>
        implements Predicate<R>, Versioned {

    private FilterByFieldPredicateConfig config;

    @Override
    public ConfigDef config() {
        return FilterByFieldPredicateConfig.CONFIG_DEF;
    }

    @Override
    public boolean test(R record) {
        String fieldValue = getFieldValue(getObject(record));

        if (OPERATION_EXCLUDE.equals(config.getOperationType())) {
            return fieldValue.matches(config.getPattern());
        }

        if (OPERATION_INCLUDE.equals(config.getOperationType())) {
            return !fieldValue.matches(config.getPattern());
        }

        throw new DataException("Invalid operation");
    }

    private String getFieldValue(Object object) {
        if (object == null) {
            throw new DataException("Key or Value should be different to null");
        }

        if (!(object instanceof Struct)) {
            throw new DataException(
                    "Expected schema structure not provided, it should be a Struct");
        }

        Struct structObject = (Struct) object;
        String fieldValue = structObject.getString(config.getField());
        if (fieldValue == null) {
            throw new DataException(String.format(
                    "Field %s value was expected but not provided", config.getField()));
        }
        return fieldValue;
    }

    @Override
    public void close() {
        // nothing to do
    }

    @Override
    public void configure(Map<String, ?> configs) {
        config = new FilterByFieldPredicateConfig(configs);
    }

    @Override
    public String version() {
        return VersionReader.version();
    }

    public abstract Object getObject(R record);

    public static class Key<T extends ConnectRecord<T>> extends FilterByFieldPredicate<T> {
        @Override
        public Object getObject(T record) {
            return record.key();
        }
    }

    public static class Value<T extends ConnectRecord<T>> extends FilterByFieldPredicate<T> {
        @Override
        public Object getObject(T record) {
            return record.value();
        }
    }
}
