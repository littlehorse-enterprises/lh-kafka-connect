package io.littlehorse.e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import io.littlehorse.e2e.configs.E2ETest;
import io.littlehorse.sdk.common.proto.LHStatus;
import io.littlehorse.sdk.common.proto.LittleHorseGrpc.LittleHorseBlockingStub;
import io.littlehorse.sdk.common.proto.SearchTaskRunRequest;
import io.littlehorse.sdk.common.proto.SearchWfRunRequest;
import io.littlehorse.sdk.common.proto.TaskRunIdList;
import io.littlehorse.sdk.common.proto.WfRunId;
import io.littlehorse.sdk.common.proto.WfRunIdList;
import io.littlehorse.sdk.wfsdk.Workflow;
import io.littlehorse.sdk.worker.LHTaskMethod;

import net.datafaker.Faker;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.stream.Stream;

public class RunWorkflowsTest extends E2ETest {

    public static final String WORKFLOW_NAME = "greetings";
    public static final String TASK_NAME = "greetings";
    private static final String TOPIC_NAME = "my-input-topic";
    private static final Workflow WORKFLOW = Workflow.newWorkflow(
            WORKFLOW_NAME, wf -> wf.execute(WORKFLOW_NAME, wf.declareStr("name")));
    private static final Faker faker = new Faker();
    public static final String CONNECTOR_NAME = "my-connector";
    private final LittleHorseBlockingStub lhClient = getLittleHorseConfig().getBlockingStub();

    @LHTaskMethod(TASK_NAME)
    public String greetings(String name) {
        String message = String.format("Hello %s!", name);
        log.info("Executing worker, output: {}", message);
        return message;
    }

    @Test
    public void shouldExecuteWfRunAfterProducing() {
        String[] inputNames =
                Stream.generate(() -> faker.starWars().character()).limit(2).toArray(String[]::new);

        startWorker(this);
        registerWorkflow(WORKFLOW);
        createTopics(TOPIC_NAME);
        produceValues(TOPIC_NAME, inputNames);
        registerConnector(CONNECTOR_NAME, getConnectorConfig());

        WfRunId wfRunId1 =
                WfRunId.newBuilder().setId("my-connector-my-input-topic-0-1").build();

        WfRunId wfRunId0 =
                WfRunId.newBuilder().setId("my-connector-my-input-topic-0-0").build();

        await(() -> {
            SearchWfRunRequest criteria = SearchWfRunRequest.newBuilder()
                    .setStatus(LHStatus.COMPLETED)
                    .setWfSpecName(WORKFLOW_NAME)
                    .build();
            WfRunIdList result = lhClient.searchWfRun(criteria);

            WfRunIdList expected = WfRunIdList.newBuilder()
                    .addResults(wfRunId1)
                    .addResults(wfRunId0)
                    .build();
            assertThat(result).isEqualTo(expected);
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
        connectorConfig.put("topics", TOPIC_NAME);
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
