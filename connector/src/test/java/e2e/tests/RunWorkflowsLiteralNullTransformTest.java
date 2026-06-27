package e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import e2e.configs.E2ETest;

import io.littlehorse.sdk.common.proto.LHStatus;
import io.littlehorse.sdk.common.proto.LittleHorseGrpc.LittleHorseBlockingStub;
import io.littlehorse.sdk.common.proto.SearchWfRunRequest;
import io.littlehorse.sdk.common.proto.Variable;
import io.littlehorse.sdk.common.proto.VariableId;
import io.littlehorse.sdk.common.proto.VariableValue;
import io.littlehorse.sdk.common.proto.WfRunId;
import io.littlehorse.sdk.common.proto.WfRunIdList;
import io.littlehorse.sdk.wfsdk.Workflow;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

/**
 * Uses the {@code LiteralMapperTransform} together with the {@code WfRunSinkConnector} to inject a
 * {@code null} value into a WfSpec input variable. The Kafka Connect REST API rejects a bare JSON
 * {@code null} config value, so the supported syntax is the unquoted {@code null} literal text,
 * which (with implicit casting enabled) the transform parses into a null value. A non-null literal
 * is injected alongside it to prove the rest of the payload is still mapped. The test asserts the
 * {@code WfRun} completes and that the variable fed by the null literal is left unset.
 */
public class RunWorkflowsLiteralNullTransformTest extends E2ETest {

    public static final String WORKFLOW_NAME = "literal-null-transform-workflow";
    public static final String CONNECTOR_NAME = "literal-null-transform-workflow";
    private static final String INPUT_TOPIC = "literal-null-transform";
    private static final String NULL_VARIABLE = "myNull";
    private static final String STR_VARIABLE = "myStr";
    private static final Workflow WORKFLOW = Workflow.newWorkflow(WORKFLOW_NAME, wf -> {
        wf.declareStr(STR_VARIABLE);
        wf.declareStr(NULL_VARIABLE);
        wf.sleepSeconds(1);
    });
    private final LittleHorseBlockingStub lhClient = getLittleHorseConfig().getBlockingStub();

    @Test
    public void shouldInjectNullLiteralAsUnsetVariable() {
        registerWorkflow(WORKFLOW);
        createTopics(INPUT_TOPIC);
        produceValues(INPUT_TOPIC, KafkaMessage.of("{}"));
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

        // The non-null literal is mapped as usual.
        Variable str = lhClient.getVariable(VariableId.newBuilder()
                .setWfRunId(wfRunId)
                .setThreadRunNumber(0)
                .setName(STR_VARIABLE)
                .build());
        assertThat(str.getValue().getStr()).isEqualTo("Tatooine");

        // The null literal leaves the variable unset (no value case).
        Variable nullVar = lhClient.getVariable(VariableId.newBuilder()
                .setWfRunId(wfRunId)
                .setThreadRunNumber(0)
                .setName(NULL_VARIABLE)
                .build());
        assertThat(nullVar.getValue().getValueCase())
                .isEqualTo(VariableValue.ValueCase.VALUE_NOT_SET);
    }

    private static HashMap<String, Object> getConnectorConfig() {
        HashMap<String, Object> connectorConfig = new HashMap<>();
        connectorConfig.put("tasks.max", 1);
        connectorConfig.put("connector.class", "io.littlehorse.connect.WfRunSinkConnector");
        connectorConfig.put("topics", INPUT_TOPIC);
        connectorConfig.put("key.converter", "org.apache.kafka.connect.storage.StringConverter");
        connectorConfig.put("value.converter", "org.apache.kafka.connect.json.JsonConverter");
        connectorConfig.put("value.converter.schemas.enable", false);
        connectorConfig.put("transforms", "literal");
        connectorConfig.put(
                "transforms.literal.type",
                "io.littlehorse.connect.transform.LiteralMapperTransform$Value");
        connectorConfig.put("transforms.literal.mapping." + STR_VARIABLE, "Tatooine");
        // The unquoted "null" literal text maps to a null value when implicit casting is enabled.
        connectorConfig.put("transforms.literal.mapping." + NULL_VARIABLE, "null");
        connectorConfig.put("lhc.api.port", 2023);
        connectorConfig.put("lhc.api.host", "littlehorse");
        connectorConfig.put("lhc.tenant.id", "default");
        connectorConfig.put("wf.spec.name", WORKFLOW_NAME);
        return connectorConfig;
    }
}
