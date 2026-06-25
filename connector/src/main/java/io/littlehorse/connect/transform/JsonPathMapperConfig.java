package io.littlehorse.connect.transform;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.InvalidJsonException;

import lombok.Getter;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.apache.kafka.common.config.ConfigException;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class JsonPathMapperConfig extends AbstractConfig {
    public static final String MAPPINGS_KEY = "mappings";

    public static final ConfigDef CONFIG_DEF = new ConfigDef()
            .define(
                    MAPPINGS_KEY,
                    Type.STRING,
                    Importance.HIGH,
                    "JSON object mapping each target (key, key.<path>, value, value.<path> or"
                            + " header.<name>) to a JSONPath expression evaluated against the"
                            + " record envelope {key, value, headers}.");

    private final Map<String, String> mappings;

    public JsonPathMapperConfig(Map<?, ?> originals) {
        super(CONFIG_DEF, originals);
        mappings = parseMappings(getString(MAPPINGS_KEY));
    }

    private static Map<String, String> parseMappings(String json) {
        Object parsed;
        try {
            parsed = Configuration.defaultConfiguration().jsonProvider().parse(json);
        } catch (InvalidJsonException e) {
            throw new ConfigException(MAPPINGS_KEY, json, "Invalid JSON: " + e.getMessage());
        }

        if (!(parsed instanceof Map<?, ?> map)) {
            throw new ConfigException(MAPPINGS_KEY, json, "Expected a JSON object.");
        }

        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getValue() == null) {
                throw new ConfigException(
                        MAPPINGS_KEY, json, "Null JSONPath for target: " + entry.getKey());
            }
            result.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }
        return result;
    }
}
