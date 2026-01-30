package e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import e2e.configs.E2ETest;

import io.littlehorse.sdk.common.LHLibUtil;
import io.littlehorse.sdk.common.proto.LHStatus;
import io.littlehorse.sdk.common.proto.LittleHorseGrpc.LittleHorseBlockingStub;
import io.littlehorse.sdk.common.proto.RunWfRequest;
import io.littlehorse.sdk.common.proto.SearchTaskRunRequest;
import io.littlehorse.sdk.common.proto.SearchWfRunRequest;
import io.littlehorse.sdk.common.proto.TaskRunIdList;
import io.littlehorse.sdk.common.proto.WfRunId;
import io.littlehorse.sdk.common.proto.WfRunIdList;
import io.littlehorse.sdk.wfsdk.Workflow;
import io.littlehorse.sdk.worker.LHTaskMethod;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RunChildWorkflowsWithHeadersTest extends E2ETest {

    public static final String CHILD_WORKFLOW_NAME = "child-workflow-with-headers";
    public static final String PARENT_WORKFLOW_NAME = "parent-workflow-with-headers";
    public static final String PARENT_WORKFLOW_ID = UUID.randomUUID().toString();
    public static final String CHILD_WORKFLOW_ID_1 = UUID.randomUUID().toString();
    public static final String CHILD_WORKFLOW_ID_2 = UUID.randomUUID().toString();
    public static final String TASK_NAME = "child-workflow-with-headers";
    public static final String CONNECTOR_NAME = "child-workflow-with-headers";
    private static final String INPUT_PARAMETER = "name";
    private static final String INPUT_TOPIC = "child-workflow-with-headers";
    private final LittleHorseBlockingStub lhClient = getLittleHorseConfig().getBlockingStub();

    private static final Workflow PARENT_WORKFLOW = Workflow.newWorkflow(
            PARENT_WORKFLOW_NAME,
            wf -> wf.execute(
                    TASK_NAME,
                    PARENT_WORKFLOW_NAME,
                    wf.declareStr(INPUT_PARAMETER).asPublic()));

    private static final Workflow CHILD_WORKFLOW = Workflow.newWorkflow(
            CHILD_WORKFLOW_NAME,
            wf -> wf.execute(
                    TASK_NAME,
                    CHILD_WORKFLOW_NAME,
                    wf.declareStr(INPUT_PARAMETER).asInherited()));

    static {
        CHILD_WORKFLOW.setParent(PARENT_WORKFLOW_NAME);
    }

    @LHTaskMethod(TASK_NAME)
    public String greetings(String wfName, String name) {
        String message = String.format("Hello %s! from %s", name, wfName);
        log.info("Executing worker, output: {}", message);
        return message;
    }

    @Test
    public void shouldExecuteWfRunAfterProducing() {
        startWorker(this);
        registerWorkflow(PARENT_WORKFLOW, CHILD_WORKFLOW);
        createTopics(INPUT_TOPIC);
        produceValues(
                INPUT_TOPIC,
                KafkaMessage.of(
                        null,
                        "{}",
                        Map.of(
                                "wfRunId",
                                CHILD_WORKFLOW_ID_1,
                                "parentWfRunId",
                                PARENT_WORKFLOW_ID)),
                KafkaMessage.of(
                        null,
                        "{}",
                        Map.of(
                                "wfRunId",
                                CHILD_WORKFLOW_ID_2,
                                "parentWfRunId",
                                PARENT_WORKFLOW_ID)));

        lhClient.runWf(RunWfRequest.newBuilder()
                .setWfSpecName(PARENT_WORKFLOW_NAME)
                .setId(PARENT_WORKFLOW_ID)
                .putVariables(INPUT_PARAMETER, LHLibUtil.objToVarVal("Leia"))
                .build());

        registerConnector(CONNECTOR_NAME, getConnectorConfig());

        await(() -> {
            SearchWfRunRequest criteria = SearchWfRunRequest.newBuilder()
                    .setStatus(LHStatus.COMPLETED)
                    .setWfSpecName(CHILD_WORKFLOW_NAME)
                    .build();
            WfRunIdList result = lhClient.searchWfRun(criteria);

            WfRunIdList expected = WfRunIdList.newBuilder()
                    .addResults(WfRunId.newBuilder()
                            .setId(CHILD_WORKFLOW_ID_1)
                            .setParentWfRunId(LHLibUtil.wfRunIdFromString(PARENT_WORKFLOW_ID))
                            .build())
                    .addResults(WfRunId.newBuilder()
                            .setId(CHILD_WORKFLOW_ID_2)
                            .setParentWfRunId(LHLibUtil.wfRunIdFromString(PARENT_WORKFLOW_ID))
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
        connectorConfig.put("topics", INPUT_TOPIC);
        connectorConfig.put("key.converter", "org.apache.kafka.connect.storage.StringConverter");
        connectorConfig.put("value.converter", "org.apache.kafka.connect.json.JsonConverter");
        connectorConfig.put("value.converter.schemas.enable", false);
        connectorConfig.put("lhc.api.port", 2023);
        connectorConfig.put("lhc.api.host", "littlehorse");
        connectorConfig.put("lhc.tenant.id", "default");
        connectorConfig.put("wf.spec.name", CHILD_WORKFLOW_NAME);
        return connectorConfig;
    }
}
