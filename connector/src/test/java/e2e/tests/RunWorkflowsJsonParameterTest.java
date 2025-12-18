package e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import e2e.configs.E2ETest;

import io.littlehorse.sdk.common.LHLibUtil;
import io.littlehorse.sdk.common.proto.LHStatus;
import io.littlehorse.sdk.common.proto.LittleHorseGrpc.LittleHorseBlockingStub;
import io.littlehorse.sdk.common.proto.SearchTaskRunRequest;
import io.littlehorse.sdk.common.proto.SearchWfRunRequest;
import io.littlehorse.sdk.common.proto.TaskRunIdList;
import io.littlehorse.sdk.common.proto.WfRunId;
import io.littlehorse.sdk.common.proto.WfRunIdList;
import io.littlehorse.sdk.wfsdk.Workflow;
import io.littlehorse.sdk.worker.LHTaskMethod;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

public class RunWorkflowsJsonParameterTest extends E2ETest {

    public static final String WORKFLOW_NAME = "json-workflow";
    public static final String TASK_NAME = "json-workflow";
    public static final String CONNECTOR_NAME = "json-workflow";
    private static final String INPUT_PARAMETER = "person";
    private static final String INPUT_TOPIC = "person";
    private static final Workflow WORKFLOW = Workflow.newWorkflow(
            WORKFLOW_NAME, wf -> wf.execute(TASK_NAME, wf.declareJsonObj(INPUT_PARAMETER)));
    private final LittleHorseBlockingStub lhClient = getLittleHorseConfig().getBlockingStub();

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
        connectorConfig.put("wf.spec.name", WORKFLOW_NAME);
        return connectorConfig;
    }

    public static class KafkaValue {
        private Person person;

        public KafkaValue() {}

        public KafkaValue(Person person) {
            this.person = person;
        }

        public Person getPerson() {
            return person;
        }

        public void setPerson(Person person) {
            this.person = person;
        }
    }

    public static class Person {
        private String firstName;
        private String secondName;

        public Person() {}

        public Person(String firstName, String secondName) {
            this.firstName = firstName;
            this.secondName = secondName;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getSecondName() {
            return secondName;
        }

        public void setSecondName(String secondName) {
            this.secondName = secondName;
        }

        @Override
        public String toString() {
            return String.format("%s %s", firstName, secondName);
        }
    }

    @LHTaskMethod(TASK_NAME)
    public String jsonWorkflow(Person person) {
        String message = String.format("Hello %s!", person);
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
                Pair.of(null, getJsonStr(new KafkaValue(new Person("Leia", "Organa")))),
                Pair.of(null, getJsonStr(new KafkaValue(new Person("Luke", null)))));
        registerConnector(CONNECTOR_NAME, getConnectorConfig());

        await(() -> {
            SearchWfRunRequest criteria = SearchWfRunRequest.newBuilder()
                    .setStatus(LHStatus.COMPLETED)
                    .setWfSpecName(WORKFLOW_NAME)
                    .build();
            WfRunIdList result = lhClient.searchWfRun(criteria);

            WfRunIdList expected = WfRunIdList.newBuilder()
                    .addResults(WfRunId.newBuilder()
                            .setId("%s-%s-0-0".formatted(CONNECTOR_NAME, INPUT_TOPIC))
                            .build())
                    .addResults(WfRunId.newBuilder()
                            .setId("%s-%s-0-1".formatted(CONNECTOR_NAME, INPUT_TOPIC))
                            .build())
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

    private String getJsonStr(Object object) {
        return LHLibUtil.serializeToJson(object).getJsonStr();
    }
}
