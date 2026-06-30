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
import io.littlehorse.sdk.wfsdk.WfRunVariable;
import io.littlehorse.sdk.wfsdk.Workflow;
import io.littlehorse.sdk.worker.LHStructDef;
import io.littlehorse.sdk.worker.LHTaskMethod;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

/**
 * Uses the {@code JsonPathMapperTransform} together with the {@code WfRunSinkConnector} to map the
 * whole record value (the root) into a single nested struct input variable ({@code pilot}, which
 * itself contains a {@code vehicle}). The record value already carries the pilot shape, so the
 * single mapping {@code mapping.pilot = $.value} assigns the entire root object to the variable and
 * the connector builds the two-level struct from it.
 */
public class RunWorkflowsJsonPathNestedStructTest extends E2ETest {

    public static final String WORKFLOW_NAME = "jsonpath-nested-struct-workflow";
    public static final String TASK_NAME = "jsonpath-nested-struct-workflow";
    public static final String CONNECTOR_NAME = "jsonpath-nested-struct-workflow";
    private static final String INPUT_PARAMETER = "pilot";
    private static final String INPUT_TOPIC = "jsonpath-nested-struct";
    private static final Workflow WORKFLOW = Workflow.newWorkflow(WORKFLOW_NAME, wf -> {
        WfRunVariable pilot = wf.declareStruct(INPUT_PARAMETER, Pilot.class);
        wf.execute(TASK_NAME, pilot);
    });
    private final LittleHorseBlockingStub lhClient = getLittleHorseConfig().getBlockingStub();

    @LHStructDef("jsonpath-nested-struct-vehicle")
    public static class Vehicle {

        private String model;

        public Vehicle() {}

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }

    @LHStructDef("jsonpath-nested-struct-pilot")
    public static class Pilot {

        private String name;
        private Vehicle vehicle;

        public Pilot() {}

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Vehicle getVehicle() {
            return vehicle;
        }

        public void setVehicle(Vehicle vehicle) {
            this.vehicle = vehicle;
        }
    }

    @LHTaskMethod(TASK_NAME)
    public String runTask(Pilot pilot) {
        String message = String.format(
                "Hello %s, you're driving a %s!",
                pilot.getName(), pilot.getVehicle().getModel());
        log.info("Executing worker, output: {}", message);
        return message;
    }

    @Test
    public void shouldMapWholeRootIntoNestedStruct() {
        registerStructDef(this, Vehicle.class, Pilot.class);
        startWorker(this);
        registerWorkflow(WORKFLOW);
        createTopics(INPUT_TOPIC);
        produceValues(
                INPUT_TOPIC,
                KafkaMessage.of("{\"name\":\"Luke\",\"vehicle\":{\"model\":\"X-wing\"}}"));
        registerConnector(CONNECTOR_NAME, getConnectorConfig());

        await(() -> {
            SearchWfRunRequest criteria = SearchWfRunRequest.newBuilder()
                    .setStatus(LHStatus.COMPLETED)
                    .setWfSpecName(WORKFLOW_NAME)
                    .build();
            WfRunIdList result = lhClient.searchWfRun(criteria);
            assertThat(result.getResultsList())
                    .contains(WfRunId.newBuilder()
                            .setId("%s-%s-0-0".formatted(CONNECTOR_NAME, INPUT_TOPIC))
                            .build());
        });

        await(() -> {
            SearchTaskRunRequest criteria =
                    SearchTaskRunRequest.newBuilder().setTaskDefName(TASK_NAME).build();
            TaskRunIdList result = lhClient.searchTaskRun(criteria);
            assertThat(result.getResultsCount()).isEqualTo(1);
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
        connectorConfig.put("transforms", "jsonPath");
        connectorConfig.put(
                "transforms.jsonPath.type",
                "io.littlehorse.connect.transform.JsonPathMapperTransform$Value");
        connectorConfig.put("transforms.jsonPath.mapping." + INPUT_PARAMETER, "$.value");
        connectorConfig.put("lhc.api.port", 2023);
        connectorConfig.put("lhc.api.host", "littlehorse");
        connectorConfig.put("lhc.tenant.id", "default");
        connectorConfig.put("wf.spec.name", WORKFLOW_NAME);
        return connectorConfig;
    }
}
