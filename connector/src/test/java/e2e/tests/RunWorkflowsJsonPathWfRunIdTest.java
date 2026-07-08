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
 * Uses the {@code JsonPathMapperTransform} together with the {@code WfRunSinkConnector} to build a
 * custom {@code WfRunId} from the record's Kafka {@code partition} and {@code offset} exposed in
 * the JSONPath envelope. Each record started with an id such as {@code p<partition>-o<offset>}.
 */
public class RunWorkflowsJsonPathWfRunIdTest extends E2ETest {

    public static final String WORKFLOW_NAME = "json-path-mapper-wf-run-id";
    public static final String CONNECTOR_NAME = "json-path-mapper-wf-run-id";
    private static final String INPUT_TOPIC = "json-path-mapper-wf-run-id";
    private static final Workflow WORKFLOW = Workflow.newWorkflow(WORKFLOW_NAME, wf -> {
        wf.declareStr("name");
        wf.sleepSeconds(1);
    });
    private final LittleHorseBlockingStub lhClient = getLittleHorseConfig().getBlockingStub();

    @Test
    public void shouldBuildWfRunIdFromPartitionAndOffset() {
        registerWorkflow(WORKFLOW);
        createTopics(INPUT_TOPIC);
        // The topic has a single partition, so records land on partition 0 with offsets 0, 1, 2.
        produceValues(
                INPUT_TOPIC,
                KafkaMessage.of("{\"name\":\"Han Solo\"}"),
                KafkaMessage.of("{\"name\":\"Leia Organa\"}"),
                KafkaMessage.of("{\"name\":\"Luke Skywalker\"}"));
        registerConnector(CONNECTOR_NAME, getConnectorConfig());

        await(() -> {
            SearchWfRunRequest criteria = SearchWfRunRequest.newBuilder()
                    .setStatus(LHStatus.COMPLETED)
                    .setWfSpecName(WORKFLOW_NAME)
                    .build();
            WfRunIdList result = lhClient.searchWfRun(criteria);
            assertThat(result.getResultsList())
                    .contains(
                            WfRunId.newBuilder().setId("p0-o0").build(),
                            WfRunId.newBuilder().setId("p0-o1").build(),
                            WfRunId.newBuilder().setId("p0-o2").build());
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
        connectorConfig.put("transforms", "wfRunIdMapper");
        connectorConfig.put(
                "transforms.wfRunIdMapper.type",
                "io.littlehorse.connect.transform.JsonPathMapperTransform$Headers");
        connectorConfig.put(
                "transforms.wfRunIdMapper.mapping.wfRunId",
                "$.concat(\"p\", $.partition, \"-o\", $.offset)");
        connectorConfig.put("lhc.api.port", 2023);
        connectorConfig.put("lhc.api.host", "littlehorse");
        connectorConfig.put("lhc.tenant.id", "default");
        connectorConfig.put("wf.spec.name", WORKFLOW_NAME);
        return connectorConfig;
    }
}
