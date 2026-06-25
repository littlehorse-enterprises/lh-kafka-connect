package io.littlehorse.connect.transform;

import static io.littlehorse.connect.transform.JsonPathMapperConfig.MAPPINGS_KEY;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

import io.littlehorse.connect.util.ObjectMapper;
import io.littlehorse.connect.util.VersionReader;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.components.Versioned;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.apache.kafka.connect.header.Header;
import org.apache.kafka.connect.header.Headers;
import org.apache.kafka.connect.transforms.Transformation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JsonPathMapper<R extends ConnectRecord<R>> implements Transformation<R>, Versioned {

    private static final String KEY_DOMAIN = "key";
    private static final String VALUE_DOMAIN = "value";
    private static final String HEADER_DOMAIN = "header";
    private static final String HEADERS_DOMAIN = "headers";

    private static final Configuration JSONPATH_CONFIG = Configuration.builder()
            .options(Option.SUPPRESS_EXCEPTIONS, Option.DEFAULT_PATH_LEAF_TO_NULL)
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();
    private List<Mapping> mappings;

    @Override
    public ConfigDef config() {
        return JsonPathMapperConfig.CONFIG_DEF;
    }

    @Override
    public void configure(Map<String, ?> configs) {
        JsonPathMapperConfig config = new JsonPathMapperConfig(configs);
        mappings = config.getMappings().entrySet().stream()
                .map(entry -> Mapping.parse(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt(mapping -> mapping.targetPath.size()))
                .collect(Collectors.toList());
    }

    @Override
    public R apply(R record) {
        DocumentContext context = JsonPath.using(JSONPATH_CONFIG).parse(buildEnvelope(record));

        Object key = record.key();
        Schema keySchema = record.keySchema();
        Object value = record.value();
        Schema valueSchema = record.valueSchema();
        Headers headers = record.headers().duplicate();

        boolean keyMapped = false;
        boolean valueMapped = false;

        for (Mapping mapping : mappings) {
            Object resolved = context.read(mapping.compiledPath);

            switch (mapping.domain) {
                case KEY_DOMAIN -> {
                    if (!keyMapped) {
                        key = null;
                        keySchema = null;
                        keyMapped = true;
                    }
                    key = write(key, mapping.targetPath, resolved);
                }
                case VALUE_DOMAIN -> {
                    if (!valueMapped) {
                        value = null;
                        valueSchema = null;
                        valueMapped = true;
                    }
                    value = write(value, mapping.targetPath, resolved);
                }
                default -> {
                    String name = mapping.targetPath.get(0);
                    headers.remove(name);
                    headers.add(name, new SchemaAndValue(null, resolved));
                }
            }
        }

        return record.newRecord(
                record.topic(),
                record.kafkaPartition(),
                keySchema,
                key,
                valueSchema,
                value,
                record.timestamp(),
                headers);
    }

    private Map<String, Object> buildEnvelope(R record) {
        Map<String, Object> envelope = new HashMap<>();
        envelope.put(KEY_DOMAIN, objectMapper.removeStruct(record.key()));
        envelope.put(VALUE_DOMAIN, objectMapper.removeStruct(record.value()));

        Map<String, Object> headers = new HashMap<>();
        for (Header header : record.headers()) {
            headers.put(header.key(), header.value());
        }
        envelope.put(HEADERS_DOMAIN, headers);
        return envelope;
    }

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

    @Override
    public void close() {
        // nothing to do
    }

    @Override
    public String version() {
        return VersionReader.version();
    }

    private static final class Mapping {
        private final String domain;
        private final List<String> targetPath;
        private final JsonPath compiledPath;

        private Mapping(String domain, List<String> targetPath, JsonPath compiledPath) {
            this.domain = domain;
            this.targetPath = targetPath;
            this.compiledPath = compiledPath;
        }

        private static Mapping parse(String target, String jsonPath) {
            String[] segments = target.split("\\.");
            String domain = segments[0];

            List<String> targetPath = new ArrayList<>();
            switch (domain) {
                case KEY_DOMAIN, VALUE_DOMAIN -> {
                    for (int i = 1; i < segments.length; i++) {
                        targetPath.add(segments[i]);
                    }
                }
                case HEADER_DOMAIN, HEADERS_DOMAIN -> {
                    if (segments.length != 2) {
                        throw new ConfigException(
                                MAPPINGS_KEY, target, "Header target must be 'header.<name>'.");
                    }
                    domain = HEADER_DOMAIN;
                    targetPath.add(segments[1]);
                }
                default ->
                    throw new ConfigException(
                            MAPPINGS_KEY, target, "Unknown target domain: " + domain);
            }

            try {
                return new Mapping(domain, targetPath, JsonPath.compile(jsonPath));
            } catch (RuntimeException e) {
                throw new ConfigException(
                        MAPPINGS_KEY, jsonPath, "Invalid JSONPath: " + e.getMessage());
            }
        }
    }
}
