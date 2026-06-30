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
import java.util.Map;

/**
 * Uses the {@code FilterByFieldPredicate$Headers} variant together with the standard {@code Filter}
 * transform to keep only the records whose {@code priority} header matches the configured pattern.
 * With {@code operation=include} only the high-priority record reaches the
 * {@code WfRunSinkConnector}; the low-priority record is filtered out and never creates a
 * {@code WfRun}.
 */
public class RunWorkflowsFieldFilterTest extends E2ETest {

    public static final String WORKFLOW_NAME = "field-filter-workflow";
    public static final String CONNECTOR_NAME = "field-filter-workflow";
    private static final String INPUT_TOPIC = "field-filter";
    private static final Workflow WORKFLOW = Workflow.newWorkflow(WORKFLOW_NAME, wf -> {
        wf.declareStr("quote");
        wf.sleepSeconds(1);
    });
    private final LittleHorseBlockingStub lhClient = getLittleHorseConfig().getBlockingStub();

    @Test
    public void shouldFilterRecordsByHeaderField() {
        registerWorkflow(WORKFLOW);
        createTopics(INPUT_TOPIC);
        // The low-priority record is produced first (offset 0) so that once the high-priority
        // record (offset 1) is processed we know the low-priority one was already evaluated.
        produceValues(
                INPUT_TOPIC,
                KafkaMessage.of(
                        null,
                        "{\"quote\":\"I have a bad feeling about this\"}",
                        Map.of("priority", "low")),
                KafkaMessage.of(
                        null,
                        "{\"quote\":\"Do or do not, there is no try\"}",
                        Map.of("priority", "high")));
        registerConnector(CONNECTOR_NAME, getConnectorConfig());

        WfRunId highPriorityWfRunId = WfRunId.newBuilder()
                .setId("%s-%s-0-1".formatted(CONNECTOR_NAME, INPUT_TOPIC))
                .build();
        WfRunId lowPriorityWfRunId = WfRunId.newBuilder()
                .setId("%s-%s-0-0".formatted(CONNECTOR_NAME, INPUT_TOPIC))
                .build();

        await(() -> {
            SearchWfRunRequest criteria = SearchWfRunRequest.newBuilder()
                    .setStatus(LHStatus.COMPLETED)
                    .setWfSpecName(WORKFLOW_NAME)
                    .build();
            WfRunIdList result = lhClient.searchWfRun(criteria);
            assertThat(result.getResultsList())
                    .contains(highPriorityWfRunId)
                    .doesNotContain(lowPriorityWfRunId);
        });
    }

    private static HashMap<String, Object> getConnectorConfig() {
        HashMap<String, Object> connectorConfig = new HashMap<>();
        connectorConfig.put("tasks.max", 1);
        connectorConfig.put("connector.class", "io.littlehorse.connect.WfRunSinkConnector");
        connectorConfig.put("topics", INPUT_TOPIC);
        connectorConfig.put("key.converter", "org.apache.kafka.connect.storage.StringConverter");
        connectorConfig.put("value.converter", "org.apache.kafka.connect.json.JsonConverter");
        connectorConfig.put("value.converter.schemas.enable", false);
        connectorConfig.put("transforms", "FilterMessage");
        connectorConfig.put(
                "transforms.FilterMessage.type", "org.apache.kafka.connect.transforms.Filter");
        connectorConfig.put("transforms.FilterMessage.predicate", "HighPriority");
        connectorConfig.put("predicates", "HighPriority");
        connectorConfig.put(
                "predicates.HighPriority.type",
                "io.littlehorse.connect.predicate.FilterByFieldPredicate$Headers");
        connectorConfig.put("predicates.HighPriority.operation", "include");
        connectorConfig.put("predicates.HighPriority.pattern", "high");
        connectorConfig.put("predicates.HighPriority.field", "priority");
        connectorConfig.put("lhc.api.port", 2023);
        connectorConfig.put("lhc.api.host", "littlehorse");
        connectorConfig.put("lhc.tenant.id", "default");
        connectorConfig.put("wf.spec.name", WORKFLOW_NAME);
        return connectorConfig;
    }
}
