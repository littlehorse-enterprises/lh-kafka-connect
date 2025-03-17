package io.littlehorse.connect.predicate;

import static io.littlehorse.connect.predicate.FilterByFieldPredicateConfig.FIELD_KEY;
import static io.littlehorse.connect.predicate.FilterByFieldPredicateConfig.OPERATION_EXCLUDE;
import static io.littlehorse.connect.predicate.FilterByFieldPredicateConfig.OPERATION_INCLUDE;
import static io.littlehorse.connect.predicate.FilterByFieldPredicateConfig.OPERATION_KEY;
import static io.littlehorse.connect.predicate.FilterByFieldPredicateConfig.PATTERN_KEY;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

class FilterByFieldPredicateTest {

    public static final String MY_WORKFLOW_NAME = "my-workflow-name";
    public static final String WF_SPEC_NAME = "wfSpecName";
    public static final String MY_TOPIC = "my-topic";

    @ParameterizedTest
    @CsvSource({
        OPERATION_EXCLUDE + "," + MY_WORKFLOW_NAME + ",true",
        OPERATION_EXCLUDE + ",not-a-workflow,false",
        OPERATION_INCLUDE + "," + MY_WORKFLOW_NAME + ",false",
        OPERATION_INCLUDE + ",not-a-workflow,true"
    })
    void shouldRemoveItIfFieldMatch(String operation, String workflowName, boolean result) {
        FilterByFieldPredicate<SinkRecord> predicate = new FilterByFieldPredicate.Key<>();
        predicate.configure(Map.of(
                PATTERN_KEY, MY_WORKFLOW_NAME, FIELD_KEY, WF_SPEC_NAME, OPERATION_KEY, operation));

        Schema schema =
                SchemaBuilder.struct().field(WF_SPEC_NAME, Schema.STRING_SCHEMA).build();
        Struct struct = new Struct(schema);
        struct.put(WF_SPEC_NAME, workflowName);

        SinkRecord record = new SinkRecord(MY_TOPIC, 0, null, struct, null, null, 0);

        assertEquals(result, predicate.test(record));
    }
}
