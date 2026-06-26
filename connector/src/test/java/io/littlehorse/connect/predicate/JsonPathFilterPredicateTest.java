package io.littlehorse.connect.predicate;

import static io.littlehorse.connect.predicate.JsonPathFilterPredicateConfig.EXPRESSION_KEY;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.Test;

import java.util.Map;

class JsonPathFilterPredicateTest {

    private static final String TOPIC = "my-topic";
    private static final String WF_SPEC_NAME = "wfSpecName";

    private JsonPathFilterPredicate<SinkRecord> predicate(String expression) {
        JsonPathFilterPredicate<SinkRecord> predicate = new JsonPathFilterPredicate<>();
        predicate.configure(Map.of(EXPRESSION_KEY, expression));
        return predicate;
    }

    private SinkRecord recordWithValue(Object value) {
        return new SinkRecord(TOPIC, 0, null, null, null, value, 0);
    }

    @Test
    void shouldMatchWhenExistenceCheckResolvesToValue() {
        SinkRecord record = recordWithValue(Map.of(WF_SPEC_NAME, "order-flow"));

        assertThat(predicate("$.value.wfSpecName").test(record)).isTrue();
    }

    @Test
    void shouldNotMatchWhenPathIsMissing() {
        SinkRecord record = recordWithValue(Map.of(WF_SPEC_NAME, "order-flow"));

        assertThat(predicate("$.value.missing").test(record)).isFalse();
    }

    @Test
    void shouldMatchWhenInlineFilterReturnsResults() {
        SinkRecord record = recordWithValue(Map.of("amount", 150));

        assertThat(predicate("$.value[?(@.amount > 100)]").test(record)).isTrue();
    }

    @Test
    void shouldNotMatchWhenInlineFilterReturnsNoResults() {
        SinkRecord record = recordWithValue(Map.of("amount", 50));

        assertThat(predicate("$.value[?(@.amount > 100)]").test(record)).isFalse();
    }

    @Test
    void shouldEvaluateBooleanPathAsItsValue() {
        assertThat(predicate("$.value.active").test(recordWithValue(Map.of("active", true))))
                .isTrue();
        assertThat(predicate("$.value.active").test(recordWithValue(Map.of("active", false))))
                .isFalse();
    }

    @Test
    void shouldEvaluateAgainstAStructValue() {
        Schema schema =
                SchemaBuilder.struct().field(WF_SPEC_NAME, Schema.STRING_SCHEMA).build();
        Struct struct = new Struct(schema).put(WF_SPEC_NAME, "order-flow");

        assertThat(predicate("$.value[?(@.wfSpecName =~ /order-.*/)]")
                        .test(recordWithValue(struct)))
                .isTrue();
    }

    @Test
    void shouldRejectNonJsonPathExpression() {
        assertThatThrownBy(() -> predicate("not-a-jsonpath")).isInstanceOf(ConfigException.class);
    }
}
