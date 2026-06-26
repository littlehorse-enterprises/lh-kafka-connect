package io.littlehorse.connect.predicate;

import lombok.Getter;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;

import java.util.Map;

@Getter
public class JsonPathFilterPredicateConfig extends AbstractConfig {

    public static final String EXPRESSION_KEY = "expression";

    public static final ConfigDef CONFIG_DEF = new ConfigDef()
            .define(
                    EXPRESSION_KEY,
                    Type.STRING,
                    ConfigDef.NO_DEFAULT_VALUE,
                    Importance.HIGH,
                    "A JSONPath expression (starting with '$') evaluated against the record"
                            + " envelope {key, value, headers}. The record matches when the result"
                            + " is truthy: a true boolean, a non-empty match list or object, or any"
                            + " other non-null value (e.g. an existence check); it does not match"
                            + " when the result is null, false, or an empty match. Combine with the"
                            + " transform's 'negate' option to invert the result.");

    private final String expression;

    public JsonPathFilterPredicateConfig(Map<?, ?> originals) {
        super(CONFIG_DEF, originals);
        expression = getString(EXPRESSION_KEY);
    }
}
