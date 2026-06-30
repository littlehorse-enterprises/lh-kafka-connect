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
 * Uses the {@code JsonPathMapperTransform} together with the {@code WfRunSinkConnector} to feed
 * values computed by JSONPath functions (a concatenated string and an aggregated sum) into typed
 * WfSpec input variables.
 */
public class RunWorkflowsJsonPathComputedTest extends E2ETest {

    public static final String WORKFLOW_NAME = "jsonpath-computed-workflow";
    public static final String CONNECTOR_NAME = "jsonpath-computed-workflow";
    private static final String INPUT_TOPIC = "jsonpath-computed";
    private static final Workflow WORKFLOW = Workflow.newWorkflow(WORKFLOW_NAME, wf -> {
        wf.declareStr("fullName");
        wf.declareDouble("total");
        wf.sleepSeconds(1);
    });
    private final LittleHorseBlockingStub lhClient = getLittleHorseConfig().getBlockingStub();

    @Test
    public void shouldMapComputedValuesIntoTypedVariables() {
        registerWorkflow(WORKFLOW);
        createTopics(INPUT_TOPIC);
        produceValues(
                INPUT_TOPIC,
                KafkaMessage.of(
                        "{\"firstName\":\"John\",\"lastName\":\"Doe\",\"subtotal\":90,\"tax\":10}"));
        registerConnector(CONNECTOR_NAME, getConnectorConfig());

        await(() -> {
            SearchWfRunRequest criteria = SearchWfRunRequest.newBuilder()
                    .setStatus(LHStatus.COMPLETED)
                    .setWfSpecName(WORKFLOW_NAME)
                    .build();
            WfRunIdList result = lhClient.searchWfRun(criteria);
            assertThat(result.getResultsList())
                    .contains(WfRunId.newBuilder()
                            .setId("%s-%s-0-0".formatted(CONNECTOR_NAME, INPUT_TOPIC))
                            .build());
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
        connectorConfig.put("transforms", "jsonPath");
        connectorConfig.put(
                "transforms.jsonPath.type",
                "io.littlehorse.connect.transform.JsonPathMapperTransform$Value");
        connectorConfig.put(
                "transforms.jsonPath.mapping.fullName",
                "$.concat($.value.firstName, \" \", $.value.lastName)");
        connectorConfig.put(
                "transforms.jsonPath.mapping.total", "$.sum($.value.subtotal, $.value.tax)");
        connectorConfig.put("lhc.api.port", 2023);
        connectorConfig.put("lhc.api.host", "littlehorse");
        connectorConfig.put("lhc.tenant.id", "default");
        connectorConfig.put("wf.spec.name", WORKFLOW_NAME);
        return connectorConfig;
    }
}
