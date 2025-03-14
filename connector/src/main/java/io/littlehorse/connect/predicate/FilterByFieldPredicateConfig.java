package io.littlehorse.connect.predicate;

import lombok.Getter;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.apache.kafka.common.config.ConfigDef.ValidString;

import java.util.Map;

@Getter
public class FilterByFieldPredicateConfig extends AbstractConfig {
    public static final String OPERATION_KEY = "operation";
    public static final String OPERATION_INCLUDE = "include";
    public static final String OPERATION_EXCLUDE = "exclude";
    public static final String PATTERN_KEY = "pattern";
    public static final String FIELD_KEY = "field";

    public static final ConfigDef CONFIG_DEF = new ConfigDef()
            .define(
                    OPERATION_KEY,
                    Type.STRING,
                    OPERATION_EXCLUDE,
                    ValidString.in(OPERATION_INCLUDE, OPERATION_EXCLUDE),
                    Importance.HIGH,
                    "Operation type, include or exclude message.")
            .define(PATTERN_KEY, Type.STRING, Importance.HIGH, "Java regex pattern.")
            .define(FIELD_KEY, Type.STRING, Importance.HIGH, "Field name.");

    private final String operationType;
    private final String pattern;
    private final String field;

    public FilterByFieldPredicateConfig(Map<?, ?> originals) {
        super(CONFIG_DEF, originals);
        operationType = getString(OPERATION_KEY);
        pattern = getString(PATTERN_KEY);
        field = getString(FIELD_KEY);
    }
}
