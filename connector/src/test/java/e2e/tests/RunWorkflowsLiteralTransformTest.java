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
 * Uses the {@code LiteralMapperTransform} together with the {@code WfRunSinkConnector} to inject
 * constant, type-inferred values into typed WfSpec input variables (string, integer, double and
 * boolean). The literal transform merges its constants onto the record value, so the payload is an
 * empty object and the resulting variables are exactly the injected constants.
 */
public class RunWorkflowsLiteralTransformTest extends E2ETest {

    public static final String WORKFLOW_NAME = "literal-transform-workflow";
    public static final String CONNECTOR_NAME = "literal-transform-workflow";
    private static final String INPUT_TOPIC = "literal-transform";
    private static final Workflow WORKFLOW = Workflow.newWorkflow(WORKFLOW_NAME, wf -> {
        wf.declareStr("myStr");
        wf.declareInt("myInt");
        wf.declareDouble("myDouble");
        wf.declareBool("myBool");
        wf.sleepSeconds(1);
    });
    private final LittleHorseBlockingStub lhClient = getLittleHorseConfig().getBlockingStub();

    @Test
    public void shouldInjectLiteralValuesIntoTypedVariables() {
        registerWorkflow(WORKFLOW);
        createTopics(INPUT_TOPIC);
        produceValues(INPUT_TOPIC, KafkaMessage.of("{}"));
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
        connectorConfig.put("transforms", "literal");
        connectorConfig.put(
                "transforms.literal.type",
                "io.littlehorse.connect.transform.LiteralMapperTransform$Value");
        connectorConfig.put("transforms.literal.mapping.myStr", "service-a");
        connectorConfig.put("transforms.literal.mapping.myInt", "42");
        connectorConfig.put("transforms.literal.mapping.myDouble", "3.14");
        connectorConfig.put("transforms.literal.mapping.myBool", "true");
        connectorConfig.put("lhc.api.port", 2023);
        connectorConfig.put("lhc.api.host", "littlehorse");
        connectorConfig.put("lhc.tenant.id", "default");
        connectorConfig.put("wf.spec.name", WORKFLOW_NAME);
        return connectorConfig;
    }
}
