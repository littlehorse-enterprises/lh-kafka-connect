package io.littlehorse.connect.predicate;

import static io.littlehorse.connect.predicate.JsonPathFilterPredicateConfig.EXPRESSION_KEY;

import com.jayway.jsonpath.JsonPath;

import io.littlehorse.connect.util.JsonPathEvaluator;
import io.littlehorse.connect.util.VersionReader;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.components.Versioned;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.transforms.predicates.Predicate;

import java.util.Collection;
import java.util.Map;

/**
 * A {@link Predicate} that matches a record by evaluating a JSONPath expression against an
 * envelope of the record: {@code {key, value, headers}}. The record matches when the result is
 * truthy: a {@code true} boolean, a non-empty match list or object, or any other non-null value.
 * It does not match when the result is {@code null}, {@code false}, or an empty match.
 *
 * <p>Unlike {@link FilterByFieldPredicate} there is no {@code $Key}/{@code $Value} variant: the
 * expression itself selects {@code $.key}, {@code $.value} or {@code $.headers}. Use the
 * transform's {@code negate} option to invert the result.
 */
public class JsonPathFilterPredicate<R extends ConnectRecord<R>>
        implements Predicate<R>, Versioned {

    private final JsonPathEvaluator evaluator = new JsonPathEvaluator();
    private JsonPath expression;

    @Override
    public ConfigDef config() {
        return JsonPathFilterPredicateConfig.CONFIG_DEF;
    }

    @Override
    public void configure(Map<String, ?> configs) {
        JsonPathFilterPredicateConfig config = new JsonPathFilterPredicateConfig(configs);
        String raw = config.getExpression();
        if (!raw.strip().startsWith("$")) {
            throw new ConfigException(
                    EXPRESSION_KEY,
                    raw,
                    "Value must be a JSONPath expression (starting with '$').");
        }
        try {
            expression = JsonPath.compile(raw);
        } catch (RuntimeException e) {
            throw new ConfigException(EXPRESSION_KEY, raw, "Invalid JSONPath: " + e.getMessage());
        }
    }

    @Override
    public boolean test(R record) {
        return isTruthy(evaluator.parse(record).read(expression));
    }

    private static boolean isTruthy(Object result) {
        if (result == null) {
            return false;
        }
        if (result instanceof Boolean booleanResult) {
            return booleanResult;
        }
        if (result instanceof Collection<?> collectionResult) {
            return !collectionResult.isEmpty();
        }
        if (result instanceof Map<?, ?> mapResult) {
            return !mapResult.isEmpty();
        }
        return true;
    }

    @Override
    public void close() {
        // nothing to do
    }

    @Override
    public String version() {
        return VersionReader.version();
    }
}
