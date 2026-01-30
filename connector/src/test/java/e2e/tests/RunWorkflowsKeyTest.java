package e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import e2e.configs.E2ETest;

import io.littlehorse.sdk.common.proto.LHStatus;
import io.littlehorse.sdk.common.proto.LittleHorseGrpc.LittleHorseBlockingStub;
import io.littlehorse.sdk.common.proto.SearchTaskRunRequest;
import io.littlehorse.sdk.common.proto.SearchWfRunRequest;
import io.littlehorse.sdk.common.proto.TaskRunIdList;
import io.littlehorse.sdk.common.proto.WfRunId;
import io.littlehorse.sdk.common.proto.WfRunIdList;
import io.littlehorse.sdk.wfsdk.Workflow;
import io.littlehorse.sdk.worker.LHTaskMethod;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class RunWorkflowsKeyTest extends E2ETest {

    public static final String WORKFLOW_NAME = "wfrun-test-with-key";
    public static final String TASK_NAME = "wfrun-test-with-key";
    public static final String CONNECTOR_NAME = "wfrun-test-with-key";
    private static final String INPUT_TOPIC = "wfrun-test-with-key";
    public static final String WF_RUN_ID_1 = UUID.randomUUID().toString();
    public static final String WF_RUN_ID_2 = UUID.randomUUID().toString();
    private static final Workflow WORKFLOW = Workflow.newWorkflow(
            WORKFLOW_NAME, wf -> wf.execute(WORKFLOW_NAME, wf.declareStr("name")));
    private final LittleHorseBlockingStub lhClient = getLittleHorseConfig().getBlockingStub();

    @LHTaskMethod(TASK_NAME)
    public String greetings(String name) {
        String message = String.format("Hello %s!", name);
        log.info("Executing worker, output: {}", message);
        return message;
    }

    @Test
    public void shouldExecuteWfRunAfterProducing() {
        startWorker(this);
        registerWorkflow(WORKFLOW);
        createTopics(INPUT_TOPIC);
        produceValues(
                INPUT_TOPIC,
                KafkaMessage.of(WF_RUN_ID_1, "Leia Organa"),
                KafkaMessage.of(WF_RUN_ID_2, "Luke Skywalker"));
        registerConnector(CONNECTOR_NAME, getConnectorConfig());

        await(() -> {
            SearchWfRunRequest criteria = SearchWfRunRequest.newBuilder()
                    .setStatus(LHStatus.COMPLETED)
                    .setWfSpecName(WORKFLOW_NAME)
                    .build();
            WfRunIdList result = lhClient.searchWfRun(criteria);
            List<WfRunId> resultsList = result.getResultsList();

            assertThat(resultsList)
                    .contains(WfRunId.newBuilder().setId(WF_RUN_ID_1).build());
            assertThat(resultsList)
                    .contains(WfRunId.newBuilder().setId(WF_RUN_ID_2).build());
        });

        await(() -> {
            SearchTaskRunRequest criteria =
                    SearchTaskRunRequest.newBuilder().setTaskDefName(TASK_NAME).build();
            TaskRunIdList result = lhClient.searchTaskRun(criteria);
            assertThat(result.getResultsCount()).isEqualTo(2);
        });
    }

    private static HashMap<String, Object> getConnectorConfig() {
        HashMap<String, Object> connectorConfig = new HashMap<>();
        connectorConfig.put("tasks.max", 1);
        connectorConfig.put("connector.class", "io.littlehorse.connect.WfRunSinkConnector");
        connectorConfig.put("topics", INPUT_TOPIC);
        connectorConfig.put("key.converter", "org.apache.kafka.connect.storage.StringConverter");
        connectorConfig.put("value.converter", "org.apache.kafka.connect.storage.StringConverter");
        connectorConfig.put("transforms", "HoistField");
        connectorConfig.put(
                "transforms.HoistField.type",
                "org.apache.kafka.connect.transforms.HoistField$Value");
        connectorConfig.put("transforms.HoistField.field", "name");
        connectorConfig.put("lhc.api.port", 2023);
        connectorConfig.put("lhc.api.host", "littlehorse");
        connectorConfig.put("lhc.tenant.id", "default");
        connectorConfig.put("wf.spec.name", WORKFLOW_NAME);
        return connectorConfig;
    }
}
