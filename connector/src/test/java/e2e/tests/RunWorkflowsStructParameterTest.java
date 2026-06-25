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
import io.littlehorse.sdk.worker.LHStructDef;
import io.littlehorse.sdk.worker.LHTaskMethod;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

public class RunWorkflowsStructParameterTest extends E2ETest {

    public static final String WORKFLOW_NAME = "struct-workflow";
    public static final String TASK_NAME = "struct-workflow";
    public static final String CONNECTOR_NAME = "struct-workflow";
    private static final String INPUT_PARAMETER = "pilot";
    private static final String INPUT_TOPIC = "pilot";
    private static final Workflow WORKFLOW = Workflow.newWorkflow(
            WORKFLOW_NAME,
            wf -> wf.execute(TASK_NAME, wf.declareStruct(INPUT_PARAMETER, Pilot.class)));
    private final LittleHorseBlockingStub lhClient = getLittleHorseConfig().getBlockingStub();

    @LHStructDef("struct-workflow-vehicle")
    public static class Vehicle {

        private String model;

        public Vehicle() {}

        public Vehicle(String model) {
            this.model = model;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }

    @LHStructDef("struct-workflow-pilot")
    public static class Pilot {

        private String name;
        private Vehicle vehicle;

        public Pilot() {}

        public Pilot(String name, Vehicle vehicle) {
            this.name = name;
            this.vehicle = vehicle;
        }

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

    public static class StructInput {

        private Pilot pilot;

        public StructInput() {}

        public StructInput(Pilot pilot) {
            this.pilot = pilot;
        }

        public Pilot getPilot() {
            return pilot;
        }

        public void setPilot(Pilot pilot) {
            this.pilot = pilot;
        }
    }

    @LHTaskMethod(TASK_NAME)
    public String structWorkflow(Pilot pilot) {
        String message = String.format(
                "Hello %s, you're driving a %s!",
                pilot.getName(), pilot.getVehicle().getModel());
        log.info("Executing worker, output: {}", message);
        return message;
    }

    @Test
    public void shouldExecuteWfRunAfterProducing() {
        registerStructDef(this, Vehicle.class, Pilot.class);
        startWorker(this);
        registerWorkflow(WORKFLOW);
        createTopics(INPUT_TOPIC);
        produceValues(
                INPUT_TOPIC,
                KafkaMessage.of(
                        getJsonStr(new StructInput(new Pilot("Luke", new Vehicle("X-wing"))))),
                KafkaMessage.of(
                        getJsonStr(new StructInput(new Pilot("Leia", new Vehicle("Tantive IV"))))));
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
}
