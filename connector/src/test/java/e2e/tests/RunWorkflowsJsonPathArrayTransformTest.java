package e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import e2e.configs.E2ETest;

import io.littlehorse.sdk.common.proto.LHStatus;
import io.littlehorse.sdk.common.proto.LittleHorseGrpc.LittleHorseBlockingStub;
import io.littlehorse.sdk.common.proto.SearchWfRunRequest;
import io.littlehorse.sdk.common.proto.Variable;
import io.littlehorse.sdk.common.proto.VariableId;
import io.littlehorse.sdk.common.proto.WfRunId;
import io.littlehorse.sdk.common.proto.WfRunIdList;
import io.littlehorse.sdk.wfsdk.Workflow;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

/**
 * Uses the {@code JsonPathMapperTransform} together with the {@code WfRunSinkConnector} to reshape
 * an array of objects. The record value carries a {@code crew} array whose elements have several
 * fields; a multi-property array projection ({@code $.value.crew[*]['firstName','role']}) rebuilds
 * every element into a slimmer {@code {firstName, role}} object. The restructured array is mapped to
 * a {@code JSON_ARR} input variable and sent to a {@code WfRun}; the test reads the variable back to
 * assert every element kept only the selected fields.
 */
public class RunWorkflowsJsonPathArrayTransformTest extends E2ETest {

    public static final String WORKFLOW_NAME = "jsonpath-array-transform-workflow";
    public static final String CONNECTOR_NAME = "jsonpath-array-transform-workflow";
    private static final String INPUT_TOPIC = "jsonpath-array-transform";
    private static final String VARIABLE_NAME = "members";
    private static final Workflow WORKFLOW = Workflow.newWorkflow(WORKFLOW_NAME, wf -> {
        wf.declareJsonArr(VARIABLE_NAME);
        wf.sleepSeconds(1);
    });
    private final LittleHorseBlockingStub lhClient = getLittleHorseConfig().getBlockingStub();

    @Test
    public void shouldReshapeEveryObjectInTheArray() {
        registerWorkflow(WORKFLOW);
        createTopics(INPUT_TOPIC);
        produceValues(
                INPUT_TOPIC,
                KafkaMessage.of("{\"crew\":["
                        + "{\"firstName\":\"Luke\",\"lastName\":\"Skywalker\",\"role\":\"pilot\",\"midichlorians\":12000},"
                        + "{\"firstName\":\"Han\",\"lastName\":\"Solo\",\"role\":\"captain\",\"midichlorians\":0},"
                        + "{\"firstName\":\"Leia\",\"lastName\":\"Organa\",\"role\":\"general\",\"midichlorians\":9000}"
                        + "]}"));
        registerConnector(CONNECTOR_NAME, getConnectorConfig());

        WfRunId wfRunId = WfRunId.newBuilder()
                .setId("%s-%s-0-0".formatted(CONNECTOR_NAME, INPUT_TOPIC))
                .build();

        await(() -> {
            SearchWfRunRequest criteria = SearchWfRunRequest.newBuilder()
                    .setStatus(LHStatus.COMPLETED)
                    .setWfSpecName(WORKFLOW_NAME)
                    .build();
            WfRunIdList result = lhClient.searchWfRun(criteria);
            assertThat(result.getResultsList()).contains(wfRunId);
        });

        Variable members = lhClient.getVariable(VariableId.newBuilder()
                .setWfRunId(wfRunId)
                .setThreadRunNumber(0)
                .setName(VARIABLE_NAME)
                .build());

        // Every element was rebuilt to {firstName, role}; lastName and midichlorians were dropped.
        assertThat(members.getValue().getJsonArr())
                .contains("Luke", "Han", "Leia")
                .contains("pilot", "captain", "general")
                .doesNotContain("lastName")
                .doesNotContain("Skywalker")
                .doesNotContain("midichlorians");
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
                "transforms.jsonPath.mapping.members", "$.value.crew[*]['firstName','role']");
        connectorConfig.put("lhc.api.port", 2023);
        connectorConfig.put("lhc.api.host", "littlehorse");
        connectorConfig.put("lhc.tenant.id", "default");
        connectorConfig.put("wf.spec.name", WORKFLOW_NAME);
        return connectorConfig;
    }
}
