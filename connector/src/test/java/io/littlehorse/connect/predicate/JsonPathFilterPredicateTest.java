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

import java.util.List;
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
    void shouldEvaluateNestedFieldPath() {
        SinkRecord record = recordWithValue(Map.of("order", Map.of("priority", "high")));

        assertThat(predicate("$.value.order.priority").test(record)).isTrue();
        assertThat(predicate("$.value.order.missing").test(record)).isFalse();
        assertThat(predicate("$.value.order[?(@.priority == 'high')]").test(record))
                .isTrue();
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
    void shouldMatchInlineFilterComparingAgainstEmptyString() {
        String expression = "$.value[?(@.propertyName == '')]";

        assertThat(predicate(expression).test(recordWithValue(Map.of("propertyName", ""))))
                .isTrue();
        assertThat(predicate(expression).test(recordWithValue(Map.of("propertyName", "text"))))
                .isFalse();
    }

    @Test
    void shouldMatchInlineFilterCheckingEmptyArrayLength() {
        String expression = "$.value[?(@.arrayName.length() == 0)]";

        assertThat(predicate(expression).test(recordWithValue(Map.of("arrayName", List.of()))))
                .isTrue();
        assertThat(predicate(expression).test(recordWithValue(Map.of("arrayName", List.of(1, 2)))))
                .isFalse();
    }

    @Test
    void shouldMatchInlineFilterNegatingMissingProperty() {
        String expression = "$.value[?(!@.key)]";

        assertThat(predicate(expression).test(recordWithValue(Map.of()))).isTrue();
        assertThat(predicate(expression).test(recordWithValue(Map.of("key", "value"))))
                .isFalse();
    }

    @Test
    void shouldEvaluateBooleanPathAsItsValue() {
        assertThat(predicate("$.value.active").test(recordWithValue(Map.of("active", true))))
                .isTrue();
        assertThat(predicate("$.value.active").test(recordWithValue(Map.of("active", false))))
                .isFalse();
    }

    @Test
    void shouldEvaluateNumberPathByItsValue() {
        assertThat(predicate("$.value.amount").test(recordWithValue(Map.of("amount", 42))))
                .isTrue();
        assertThat(predicate("$.value.amount").test(recordWithValue(Map.of("amount", 0))))
                .isFalse();
    }

    @Test
    void shouldEvaluateStringPathByItsEmptiness() {
        assertThat(predicate("$.value.note").test(recordWithValue(Map.of("note", "text"))))
                .isTrue();
        assertThat(predicate("$.value.note").test(recordWithValue(Map.of("note", ""))))
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
