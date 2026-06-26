package io.littlehorse.connect.transform;

import io.littlehorse.connect.util.VersionReader;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.components.Versioned;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.header.ConnectHeaders;
import org.apache.kafka.connect.transforms.Transformation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Shared machinery for the mapper transforms. Each transform builds an operating domain (the
 * record key, value, or headers) from a set of {@code mapping.<target>=<value>} properties.
 * The concrete transforms differ only in how a mapping value is resolved: {@link
 * JsonPathMapperTransform} evaluates JSONPath against the record, while {@link
 * LiteralMapperTransform} injects constant values.
 */
public abstract class AbstractMapperTransform<R extends ConnectRecord<R>>
        implements Transformation<R>, Versioned {

    private List<Mapping> mappings;

    @Override
    public ConfigDef config() {
        return MapperTransformConfig.CONFIG_DEF;
    }

    @Override
    public void configure(Map<String, ?> configs) {
        MapperTransformConfig config = new MapperTransformConfig(configs);
        mappings = config.getMappings().entrySet().stream()
                .map(entry -> compile(parseTarget(entry.getKey()), entry.getValue()))
                .sorted(Comparator.comparingInt(mapping -> mapping.targetPath.size()))
                .collect(Collectors.toList());
    }

    @Override
    public R apply(R record) {
        Object context = createContext(record);
        Object updatedValue = null;
        for (Mapping mapping : mappings) {
            updatedValue = write(updatedValue, mapping.targetPath, mapping.resolve(context));
        }
        return newRecord(record, updatedValue);
    }

    /**
     * Splits a mapping target into the path of nested fields to build within the operating
     * domain. The default splits on {@code .} so nested objects can be constructed. The
     * {@code Headers} variants override this via {@link #flatTarget(String)} to treat the whole
     * target as a single, flat header name.
     */
    protected List<String> parseTarget(String target) {
        List<String> targetPath = new ArrayList<>();
        if (target != null && !target.isBlank()) {
            for (String segment : target.split("\\.")) {
                targetPath.add(segment);
            }
        }
        return targetPath;
    }

    /** Header names are flat strings, so the whole target becomes a single literal name. */
    protected static List<String> flatTarget(String target) {
        List<String> targetPath = new ArrayList<>();
        if (target != null && !target.isBlank()) {
            targetPath.add(target);
        }
        return targetPath;
    }

    /** Builds a per-record resolution context shared by all mappings; may be {@code null}. */
    protected abstract Object createContext(R record);

    /** Compiles (and validates) a single mapping value. */
    protected abstract Mapping compile(List<String> targetPath, String value);

    /** Writes the rebuilt operating domain back into a new record. */
    protected abstract R newRecord(R record, Object updatedValue);

    @SuppressWarnings("unchecked")
    private Object write(Object base, List<String> path, Object value) {
        if (path.isEmpty()) {
            return value;
        }

        Map<String, Object> root =
                (base instanceof Map) ? (Map<String, Object>) base : new LinkedHashMap<>();
        Map<String, Object> current = root;
        for (int i = 0; i < path.size() - 1; i++) {
            Object next = current.get(path.get(i));
            if (!(next instanceof Map)) {
                next = new LinkedHashMap<String, Object>();
                current.put(path.get(i), next);
            }
            current = (Map<String, Object>) next;
        }
        current.put(path.get(path.size() - 1), value);
        return root;
    }

    protected final R asKey(R record, Object updatedValue) {
        return record.newRecord(
                record.topic(),
                record.kafkaPartition(),
                null,
                updatedValue,
                record.valueSchema(),
                record.value(),
                record.timestamp(),
                record.headers());
    }

    protected final R asValue(R record, Object updatedValue) {
        return record.newRecord(
                record.topic(),
                record.kafkaPartition(),
                record.keySchema(),
                record.key(),
                null,
                updatedValue,
                record.timestamp(),
                record.headers());
    }

    @SuppressWarnings("unchecked")
    protected final R asHeaders(R record, Object updatedValue) {
        ConnectHeaders headers = new ConnectHeaders();
        if (updatedValue instanceof Map) {
            for (Map.Entry<String, Object> entry :
                    ((Map<String, Object>) updatedValue).entrySet()) {
                headers.add(entry.getKey(), entry.getValue(), null);
            }
        }
        return record.newRecord(
                record.topic(),
                record.kafkaPartition(),
                record.keySchema(),
                record.key(),
                record.valueSchema(),
                record.value(),
                record.timestamp(),
                headers);
    }

    @Override
    public void close() {
        // nothing to do
    }

    @Override
    public String version() {
        return VersionReader.version();
    }

    /** A single target path plus the strategy to resolve its value for a record. */
    protected abstract static class Mapping {
        protected final List<String> targetPath;

        protected Mapping(List<String> targetPath) {
            this.targetPath = targetPath;
        }

        protected abstract Object resolve(Object context);
    }
}
