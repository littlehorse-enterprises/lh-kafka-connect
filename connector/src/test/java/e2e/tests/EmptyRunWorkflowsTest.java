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
import io.littlehorse.sdk.worker.WorkerContext;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

public class EmptyRunWorkflowsTest extends E2ETest {

    public static final String WORKFLOW_NAME = "empty-run-workflow";
    public static final String TASK_NAME = "empty-run-workflow";
    public static final String CONNECTOR_NAME = "empty-run-workflow";
    private static final String INPUT_TOPIC = "empty-run-workflow";
    private static final Workflow WORKFLOW =
            Workflow.newWorkflow(WORKFLOW_NAME, wf -> wf.execute(TASK_NAME));
    private final LittleHorseBlockingStub lhClient = getLittleHorseConfig().getBlockingStub();

    private static HashMap<String, Object> getConnectorConfig() {
        HashMap<String, Object> connectorConfig = new HashMap<>();
        connectorConfig.put("tasks.max", 1);
        connectorConfig.put("connector.class", "io.littlehorse.connect.WfRunSinkConnector");
        connectorConfig.put("topics", INPUT_TOPIC);
        connectorConfig.put("key.converter", "org.apache.kafka.connect.storage.StringConverter");
        connectorConfig.put("lhc.api.port", 2023);
        connectorConfig.put("lhc.api.host", "littlehorse");
        connectorConfig.put("lhc.tenant.id", "default");
        connectorConfig.put("wf.spec.name", WORKFLOW_NAME);
        return connectorConfig;
    }

    @LHTaskMethod(TASK_NAME)
    public String testTask(WorkerContext context) {
        String message = String.format(
                "Hello World!, this is a wf without variables, wfRunId: %s",
                context.getWfRunId().getId());
        log.info("Executing worker, output: {}", message);
        return message;
    }

    @Test
    public void shouldExecuteWfRunAfterProducing() {
        startWorker(this);
        registerWorkflow(WORKFLOW);
        createTopics(INPUT_TOPIC);
        produceValues(INPUT_TOPIC, KafkaMessage.of(null), KafkaMessage.of(null));
        registerConnector(CONNECTOR_NAME, getConnectorConfig());

        await(() -> {
            SearchWfRunRequest criteria = SearchWfRunRequest.newBuilder()
                    .setStatus(LHStatus.COMPLETED)
                    .setWfSpecName(WORKFLOW_NAME)
                    .build();
            WfRunIdList result = lhClient.searchWfRun(criteria);
            List<WfRunId> resultsList = result.getResultsList();

            assertThat(resultsList)
                    .contains(WfRunId.newBuilder()
                            .setId("%s-%s-0-0".formatted(CONNECTOR_NAME, INPUT_TOPIC))
                            .build());

            assertThat(resultsList)
                    .contains(WfRunId.newBuilder()
                            .setId("%s-%s-0-1".formatted(CONNECTOR_NAME, INPUT_TOPIC))
                            .build());
        });

        await(() -> {
            SearchTaskRunRequest criteria =
                    SearchTaskRunRequest.newBuilder().setTaskDefName(TASK_NAME).build();
            TaskRunIdList result = lhClient.searchTaskRun(criteria);
            assertThat(result.getResultsCount()).isEqualTo(2);
        });
    }
}
