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
import java.util.Map;
import java.util.UUID;

public class RunWorkflowsWithWfRunIdHeaderTest extends E2ETest {

    public static final String WORKFLOW_NAME = "wf-run-printer";
    public static final String TASK_NAME = "wf-run-printer";
    public static final String CONNECTOR_NAME = "wf-run-printer";
    private static final String INPUT_TOPIC = "message-with-header";

    private static final Workflow WORKFLOW =
            Workflow.newWorkflow(WORKFLOW_NAME, wf -> wf.execute(TASK_NAME));
    private final LittleHorseBlockingStub lhClient = getLittleHorseConfig().getBlockingStub();

    private static HashMap<String, Object> getConnectorConfig() {
        HashMap<String, Object> connectorConfig = new HashMap<>();
        connectorConfig.put("tasks.max", 1);
        connectorConfig.put("connector.class", "io.littlehorse.connect.WfRunSinkConnector");
        connectorConfig.put("topics", INPUT_TOPIC);
        connectorConfig.put("key.converter", "org.apache.kafka.connect.storage.StringConverter");
        connectorConfig.put("value.converter", "org.apache.kafka.connect.json.JsonConverter");
        connectorConfig.put("value.converter.schemas.enable", false);
        connectorConfig.put("header.converter", "org.apache.kafka.connect.storage.StringConverter");
        connectorConfig.put("lhc.api.port", 2023);
        connectorConfig.put("lhc.api.host", "littlehorse");
        connectorConfig.put("lhc.tenant.id", "default");
        connectorConfig.put("wf.spec.name", WORKFLOW_NAME);
        return connectorConfig;
    }

    @LHTaskMethod(TASK_NAME)
    public void wfRunIdPrinter(WorkerContext context) {
        log.info("Executing worker, WfRunId: {}", context.getWfRunId().getId());
    }

    @Test
    public void shouldExecuteWfRunAfterProducing() {
        String wfRunId1 = UUID.randomUUID().toString();
        String wfRunId2 = UUID.randomUUID().toString();

        startWorker(this);
        registerWorkflow(WORKFLOW);
        createTopics(INPUT_TOPIC);
        produceValues(
                INPUT_TOPIC,
                KafkaMessage.of(null, "{}", Map.of("wfRunId", wfRunId1)),
                KafkaMessage.of(null, "{}", Map.of("wfRunId", wfRunId2)));
        registerConnector(CONNECTOR_NAME, getConnectorConfig());

        await(() -> {
            SearchWfRunRequest criteria = SearchWfRunRequest.newBuilder()
                    .setStatus(LHStatus.COMPLETED)
                    .setWfSpecName(WORKFLOW_NAME)
                    .build();
            WfRunIdList result = lhClient.searchWfRun(criteria);
            List<WfRunId> resultsList = result.getResultsList();

            assertThat(resultsList)
                    .contains(WfRunId.newBuilder().setId(wfRunId1).build());
            assertThat(resultsList)
                    .contains(WfRunId.newBuilder().setId(wfRunId2).build());
        });

        await(() -> {
            SearchTaskRunRequest criteria =
                    SearchTaskRunRequest.newBuilder().setTaskDefName(TASK_NAME).build();
            TaskRunIdList result = lhClient.searchTaskRun(criteria);
            assertThat(result.getResultsCount()).isEqualTo(2);
        });
    }
}
