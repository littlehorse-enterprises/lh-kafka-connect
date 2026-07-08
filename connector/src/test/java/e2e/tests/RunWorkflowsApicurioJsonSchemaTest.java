package e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
import java.util.Map;

/**
 * Verifies the {@code WfRunSinkConnector} can consume records serialized with Apicurio Registry's
 * JSON Schema serde by using the connector's own {@code JsonSchemaKafkaConverter}. A permissive JSON
 * Schema artifact is registered in the registry, records are produced with the Apicurio
 * {@code JsonSchemaKafkaSerializer}, and the connector deserializes them through the converter
 * (which resolves the schema from the registry) and runs a {@code WfRun} per record.
 */
public class RunWorkflowsApicurioJsonSchemaTest extends E2ETest {

    public static final String WORKFLOW_NAME = "apicurio-json-schema-workflow";
    public static final String TASK_NAME = "apicurio-json-schema-workflow";
    public static final String CONNECTOR_NAME = "apicurio-json-schema-workflow";
    private static final String INPUT_PARAMETER = "person";
    private static final String INPUT_TOPIC = "apicurio-json-schema";
    private static final String ARTIFACT_ID = INPUT_TOPIC + "-value";
    private static final String JSON_SCHEMA =
            "{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"object\"}";

    private static final Workflow WORKFLOW = Workflow.newWorkflow(
            WORKFLOW_NAME, wf -> wf.execute(TASK_NAME, wf.declareJsonObj(INPUT_PARAMETER)));
    private final LittleHorseBlockingStub lhClient = getLittleHorseConfig().getBlockingStub();
    private final ObjectMapper mapper = new ObjectMapper();

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
    public String greet(Person person) {
        String message = String.format("Hello %s!", person);
        log.info("Executing worker, output: {}", message);
        return message;
    }

    @Test
    public void shouldExecuteWfRunFromApicurioJsonSchemaRecords() {
        startWorker(this);
        registerWorkflow(WORKFLOW);
        registerJsonSchema(ARTIFACT_ID, JSON_SCHEMA);
        createTopics(INPUT_TOPIC);

        produceJsonSchemaValues(
                INPUT_TOPIC, personEnvelope("Leia", "Organa"), personEnvelope("Luke", null));

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

    private JsonNode personEnvelope(String firstName, String secondName) {
        return mapper.valueToTree(Map.of(INPUT_PARAMETER, new Person(firstName, secondName)));
    }

    private Map<String, Object> getConnectorConfig() {
        Map<String, Object> connectorConfig = new HashMap<>();
        connectorConfig.put("tasks.max", 1);
        connectorConfig.put("connector.class", "io.littlehorse.connect.WfRunSinkConnector");
        connectorConfig.put("topics", INPUT_TOPIC);
        connectorConfig.put("key.converter", "org.apache.kafka.connect.storage.StringConverter");
        connectorConfig.put(
                "value.converter",
                "io.littlehorse.connect.converter.apicurio.JsonSchemaKafkaConverter");
        connectorConfig.put(
                "value.converter.apicurio.registry.url", getApicurioRegistryInternalUrl());
        connectorConfig.put("lhc.api.port", 2023);
        connectorConfig.put("lhc.api.host", "littlehorse");
        connectorConfig.put("lhc.tenant.id", "default");
        connectorConfig.put("wf.spec.name", WORKFLOW_NAME);
        return connectorConfig;
    }
}
