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

public class RunWorkflowsTest extends E2ETest {

    public static final String WORKFLOW_NAME = "wf-run-test";
    public static final String TASK_NAME = "wf-run-test";
    public static final String CONNECTOR_NAME = "wf-run-test";
    private static final String INPUT_TOPIC_1 = "wf-run-test";
    private static final String INPUT_TOPIC_2 = "ThisIs_A-valid.topic-1";
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
        createTopics(INPUT_TOPIC_1, INPUT_TOPIC_2);
        produceValues(
                INPUT_TOPIC_1, KafkaMessage.of("Leia Organa"), KafkaMessage.of("Luke Skywalker"));
        produceValues(INPUT_TOPIC_2, KafkaMessage.of("Anakin Skywalker"));
        registerConnector(CONNECTOR_NAME, getConnectorConfig());

        await(() -> {
            SearchWfRunRequest criteria = SearchWfRunRequest.newBuilder()
                    .setStatus(LHStatus.COMPLETED)
                    .setWfSpecName(WORKFLOW_NAME)
                    .build();
            WfRunIdList result = lhClient.searchWfRun(criteria);

            WfRunIdList expected = WfRunIdList.newBuilder()
                    .addResults(WfRunId.newBuilder()
                            .setId("%s-%s-0-0".formatted(CONNECTOR_NAME, INPUT_TOPIC_1))
                            .build())
                    .addResults(WfRunId.newBuilder()
                            .setId("%s-%s-0-1".formatted(CONNECTOR_NAME, INPUT_TOPIC_1))
                            .build())
                    .addResults(WfRunId.newBuilder()
                            .setId("%s-%s-0-0"
                                    .formatted(
                                            CONNECTOR_NAME,
                                            INPUT_TOPIC_2
                                                    .toLowerCase()
                                                    .replace("_", "-")
                                                    .replace(".", "-")))
                            .build())
                    .build();
            assertThat(result).isEqualTo(expected);
        });

        await(() -> {
            SearchTaskRunRequest criteria =
                    SearchTaskRunRequest.newBuilder().setTaskDefName(TASK_NAME).build();
            TaskRunIdList result = lhClient.searchTaskRun(criteria);
            assertThat(result.getResultsCount()).isEqualTo(3);
        });
    }

    private static HashMap<String, Object> getConnectorConfig() {
        HashMap<String, Object> connectorConfig = new HashMap<>();
        connectorConfig.put("tasks.max", 1);
        connectorConfig.put("connector.class", "io.littlehorse.connect.WfRunSinkConnector");
        connectorConfig.put("topics", INPUT_TOPIC_1 + "," + INPUT_TOPIC_2);
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
