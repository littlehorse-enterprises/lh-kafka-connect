package e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import e2e.configs.E2ETest;

import io.littlehorse.sdk.common.proto.LHStatus;
import io.littlehorse.sdk.common.proto.LittleHorseGrpc.LittleHorseBlockingStub;
import io.littlehorse.sdk.common.proto.SearchWfRunRequest;
import io.littlehorse.sdk.common.proto.WfRunId;
import io.littlehorse.sdk.common.proto.WfRunIdList;
import io.littlehorse.sdk.wfsdk.Workflow;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

/**
 * Exercises the truthiness rules of {@code JsonPathFilterPredicate} end to end. The predicate keeps
 * a record (via {@code Filter} with {@code negate=true}) only when its JSONPath expression resolves
 * to a truthy result, so a {@code WfRun} is created exactly for the matching records.
 *
 * <p>The first test pins every branch of the value-type matrix in a single connector: booleans,
 * numbers (where {@code 0} is falsy), strings (where {@code ""} is falsy), lists and objects (empty
 * is falsy), JSON {@code null}, and a missing path. The second test exercises the {@code $.key}
 * envelope to confirm the expression can select the record key.
 */
public class RunWorkflowsJsonPathFilterTruthinessTest extends E2ETest {

    private static final String WORKFLOW_NAME = "jsonpath-filter-truthiness-workflow";
    private static final String CONNECTOR_NAME = "jsonpath-filter-truthiness-workflow";
    private static final String INPUT_TOPIC = "jsonpath-filter-truthiness";

    private static final String KEY_WORKFLOW_NAME = "jsonpath-filter-key-workflow";
    private static final String KEY_CONNECTOR_NAME = "jsonpath-filter-key-workflow";
    private static final String KEY_TOPIC = "jsonpath-filter-key";

    private static final Workflow WORKFLOW = Workflow.newWorkflow(WORKFLOW_NAME, wf -> {
        wf.declareStr("quote");
        wf.sleepSeconds(1);
    });
    private static final Workflow KEY_WORKFLOW = Workflow.newWorkflow(KEY_WORKFLOW_NAME, wf -> {
        wf.declareStr("quote");
        wf.sleepSeconds(1);
    });

    private final LittleHorseBlockingStub lhClient = getLittleHorseConfig().getBlockingStub();

    @Test
    public void shouldKeepOnlyRecordsWithTruthyValueExpression() {
        registerWorkflow(WORKFLOW);
        createTopics(INPUT_TOPIC);
        // Offsets are produced in order on a single-partition topic, so each record's WfRunId is
        // "<connector>-<topic>-0-<offset>". Matching (truthy) records create a WfRun; the rest are
        // filtered out and never reach the connector.
        produceValues(
                INPUT_TOPIC,
                KafkaMessage.of("{\"quote\":\"q0\",\"check\":false}"), // 0  boolean false  -> drop
                KafkaMessage.of("{\"quote\":\"q1\",\"check\":true}"), // 1  boolean true   -> keep
                KafkaMessage.of("{\"quote\":\"q2\",\"check\":[]}"), // 2  empty list     -> drop
                KafkaMessage.of("{\"quote\":\"q3\",\"check\":[1,2]}"), // 3  non-empty list -> keep
                KafkaMessage.of("{\"quote\":\"q4\",\"check\":{}}"), // 4  empty object   -> drop
                KafkaMessage.of(
                        "{\"quote\":\"q5\",\"check\":{\"a\":1}}"), // 5  non-empty obj  -> keep
                KafkaMessage.of("{\"quote\":\"q6\",\"check\":0}"), // 6  zero number    -> drop
                KafkaMessage.of("{\"quote\":\"q7\",\"check\":42}"), // 7  non-zero number-> keep
                KafkaMessage.of("{\"quote\":\"q8\",\"check\":\"\"}"), // 8  empty string   -> drop
                KafkaMessage.of(
                        "{\"quote\":\"q9\",\"check\":\"text\"}"), // 9  non-empty str  -> keep
                KafkaMessage.of("{\"quote\":\"q10\",\"check\":null}"), // 10 json null      -> drop
                KafkaMessage.of("{\"quote\":\"q11\"}")); // 11 missing path   -> drop
        registerConnector(CONNECTOR_NAME, getValueConnectorConfig());

        await(() -> {
            WfRunIdList result = lhClient.searchWfRun(SearchWfRunRequest.newBuilder()
                    .setStatus(LHStatus.COMPLETED)
                    .setWfSpecName(WORKFLOW_NAME)
                    .build());
            assertThat(result.getResultsList())
                    .contains(
                            wfRunId(CONNECTOR_NAME, INPUT_TOPIC, 1),
                            wfRunId(CONNECTOR_NAME, INPUT_TOPIC, 3),
                            wfRunId(CONNECTOR_NAME, INPUT_TOPIC, 5),
                            wfRunId(CONNECTOR_NAME, INPUT_TOPIC, 7),
                            wfRunId(CONNECTOR_NAME, INPUT_TOPIC, 9))
                    .doesNotContain(
                            wfRunId(CONNECTOR_NAME, INPUT_TOPIC, 0),
                            wfRunId(CONNECTOR_NAME, INPUT_TOPIC, 2),
                            wfRunId(CONNECTOR_NAME, INPUT_TOPIC, 4),
                            wfRunId(CONNECTOR_NAME, INPUT_TOPIC, 6),
                            wfRunId(CONNECTOR_NAME, INPUT_TOPIC, 8),
                            wfRunId(CONNECTOR_NAME, INPUT_TOPIC, 10),
                            wfRunId(CONNECTOR_NAME, INPUT_TOPIC, 11));
        });
    }

    @Test
    public void shouldKeepOnlyRecordsWithTruthyKeyExpression() {
        registerWorkflow(KEY_WORKFLOW);
        createTopics(KEY_TOPIC);
        // The non-empty key is kept and becomes the WfRunId; the empty-string key is falsy, so the
        // record is filtered out and no WfRun is created for it.
        produceValues(
                KEY_TOPIC,
                KafkaMessage.of("keep-me", "{\"quote\":\"kept\"}"),
                KafkaMessage.of("", "{\"quote\":\"dropped\"}"));
        registerConnector(KEY_CONNECTOR_NAME, getKeyConnectorConfig());

        await(() -> {
            WfRunIdList result = lhClient.searchWfRun(SearchWfRunRequest.newBuilder()
                    .setStatus(LHStatus.COMPLETED)
                    .setWfSpecName(KEY_WORKFLOW_NAME)
                    .build());
            assertThat(result.getResultsList())
                    .containsExactly(WfRunId.newBuilder().setId("keep-me").build());
        });
    }

    private static WfRunId wfRunId(String connector, String topic, int offset) {
        return WfRunId.newBuilder()
                .setId("%s-%s-0-%d".formatted(connector, topic, offset))
                .build();
    }

    private static HashMap<String, Object> getValueConnectorConfig() {
        HashMap<String, Object> connectorConfig = baseConnectorConfig(INPUT_TOPIC, WORKFLOW_NAME);
        connectorConfig.put("transforms", "FilterMessage,ReplaceField");
        connectorConfig.put(
                "transforms.FilterMessage.type", "org.apache.kafka.connect.transforms.Filter");
        connectorConfig.put("transforms.FilterMessage.predicate", "Truthy");
        connectorConfig.put("transforms.FilterMessage.negate", true);
        connectorConfig.put(
                "transforms.ReplaceField.type",
                "org.apache.kafka.connect.transforms.ReplaceField$Value");
        connectorConfig.put("transforms.ReplaceField.exclude", "check");
        connectorConfig.put("predicates", "Truthy");
        connectorConfig.put(
                "predicates.Truthy.type",
                "io.littlehorse.connect.predicate.JsonPathFilterPredicate");
        connectorConfig.put("predicates.Truthy.expression", "$.value.check");
        return connectorConfig;
    }

    private static HashMap<String, Object> getKeyConnectorConfig() {
        HashMap<String, Object> connectorConfig = baseConnectorConfig(KEY_TOPIC, KEY_WORKFLOW_NAME);
        connectorConfig.put("transforms", "FilterMessage");
        connectorConfig.put(
                "transforms.FilterMessage.type", "org.apache.kafka.connect.transforms.Filter");
        connectorConfig.put("transforms.FilterMessage.predicate", "KeyTruthy");
        connectorConfig.put("transforms.FilterMessage.negate", true);
        connectorConfig.put("predicates", "KeyTruthy");
        connectorConfig.put(
                "predicates.KeyTruthy.type",
                "io.littlehorse.connect.predicate.JsonPathFilterPredicate");
        connectorConfig.put("predicates.KeyTruthy.expression", "$.key");
        return connectorConfig;
    }

    private static HashMap<String, Object> baseConnectorConfig(String topic, String wfSpecName) {
        HashMap<String, Object> connectorConfig = new HashMap<>();
        connectorConfig.put("tasks.max", 1);
        connectorConfig.put("connector.class", "io.littlehorse.connect.WfRunSinkConnector");
        connectorConfig.put("topics", topic);
        connectorConfig.put("key.converter", "org.apache.kafka.connect.storage.StringConverter");
        connectorConfig.put("value.converter", "org.apache.kafka.connect.json.JsonConverter");
        connectorConfig.put("value.converter.schemas.enable", false);
        connectorConfig.put("lhc.api.port", 2023);
        connectorConfig.put("lhc.api.host", "littlehorse");
        connectorConfig.put("lhc.tenant.id", "default");
        connectorConfig.put("wf.spec.name", wfSpecName);
        return connectorConfig;
    }
}
