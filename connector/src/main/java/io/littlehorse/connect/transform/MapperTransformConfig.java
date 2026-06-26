package io.littlehorse.connect.transform;

import lombok.Getter;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.apache.kafka.common.config.ConfigException;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class MapperTransformConfig extends AbstractConfig {

    /**
     * Each mapping is supplied as its own property: {@code mapping.<target>=<value>}. The
     * {@code <target>} is the path within the operating domain (a dotted path such as
     * {@code <field>.<nested>}), and the bare {@code mapping} key targets the whole key/value.
     * Use the {@code $Key} variant to write the record key, {@code $Value} for the value, and
     * {@code $Headers} for the headers.
     */
    public static final String MAPPING_KEY = "mapping";

    public static final String MAPPING_PREFIX = MAPPING_KEY + ".";

    private final Map<String, String> mappings;

    public MapperTransformConfig(ConfigDef configDef, Map<?, ?> originals) {
        super(configDef, originals);
        mappings = parseMappings();
    }

    /** Builds the {@link ConfigDef} for a mapper transform, documenting its {@code mapping} key. */
    public static ConfigDef configDef(String mappingDoc) {
        return new ConfigDef().define(MAPPING_KEY, Type.STRING, null, Importance.HIGH, mappingDoc);
    }

    private Map<String, String> parseMappings() {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : originals().entrySet()) {
            String key = entry.getKey();

            String target;
            if (key.equals(MAPPING_KEY)) {
                target = "";
            } else if (key.startsWith(MAPPING_PREFIX)) {
                target = key.substring(MAPPING_PREFIX.length());
            } else {
                continue;
            }

            if (entry.getValue() == null) {
                throw new ConfigException(key, null, "Mapping value must not be null.");
            }
            result.put(target, String.valueOf(entry.getValue()));
        }
        return result;
    }
}
