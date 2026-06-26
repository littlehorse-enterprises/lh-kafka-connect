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
import java.util.List;
import java.util.UUID;

public class RunWorkflowsJsonPathKeyTest extends E2ETest {

    public static final String WORKFLOW_NAME = "json-path-mapper-key";
    public static final String CONNECTOR_NAME = "json-path-mapper-key";
    private static final String INPUT_TOPIC = "json-path-mapper-key";
    private static final String WF_RUN_ID = UUID.randomUUID().toString();
    private static final Workflow WORKFLOW =
            Workflow.newWorkflow(WORKFLOW_NAME, wf -> wf.sleepSeconds(1));
    private final LittleHorseBlockingStub lhClient = getLittleHorseConfig().getBlockingStub();

    @Test
    public void shouldMapJsonKeyToStringKeyWithNullValue() {
        registerWorkflow(WORKFLOW);
        createTopics(INPUT_TOPIC);
        produceValues(INPUT_TOPIC, KafkaMessage.of("{\"id\":\"%s\"}".formatted(WF_RUN_ID), null));
        registerConnector(CONNECTOR_NAME, getConnectorConfig());

        await(() -> {
            SearchWfRunRequest criteria = SearchWfRunRequest.newBuilder()
                    .setStatus(LHStatus.COMPLETED)
                    .setWfSpecName(WORKFLOW_NAME)
                    .build();
            WfRunIdList result = lhClient.searchWfRun(criteria);
            List<WfRunId> resultsList = result.getResultsList();

            assertThat(resultsList)
                    .contains(WfRunId.newBuilder().setId(WF_RUN_ID).build());
        });
    }

    private static HashMap<String, Object> getConnectorConfig() {
        HashMap<String, Object> connectorConfig = new HashMap<>();
        connectorConfig.put("tasks.max", 1);
        connectorConfig.put("connector.class", "io.littlehorse.connect.WfRunSinkConnector");
        connectorConfig.put("topics", INPUT_TOPIC);
        connectorConfig.put("key.converter", "org.apache.kafka.connect.json.JsonConverter");
        connectorConfig.put("key.converter.schemas.enable", false);
        connectorConfig.put("value.converter", "org.apache.kafka.connect.storage.StringConverter");
        connectorConfig.put("transforms", "jsonPathMapper");
        connectorConfig.put(
                "transforms.jsonPathMapper.type",
                "io.littlehorse.connect.transform.JsonPathMapperTransform$Key");
        connectorConfig.put("transforms.jsonPathMapper.mapping", "$.key.id");
        connectorConfig.put("lhc.api.port", 2023);
        connectorConfig.put("lhc.api.host", "littlehorse");
        connectorConfig.put("lhc.tenant.id", "default");
        connectorConfig.put("wf.spec.name", WORKFLOW_NAME);
        return connectorConfig;
    }
}
