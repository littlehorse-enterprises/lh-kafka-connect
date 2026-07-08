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
import java.util.List;
import java.util.Map;

/**
 * Verifies the {@code WfRunSinkConnector} can consume records serialized with Apicurio Registry's
 * JSON Schema serde when the value schema uses a JSON Schema {@code $ref}. A {@code Vehicle} schema
 * is registered, then a {@code Pilot} value schema that references it; records are produced with the
 * Apicurio {@code JsonSchemaKafkaSerializer} and the connector deserializes them through the
 * {@code JsonSchemaKafkaConverter} (which resolves the referenced schema from the registry) and runs
 * a {@code WfRun} per record.
 */
public class RunWorkflowsApicurioJsonSchemaReferenceTest extends E2ETest {

    public static final String WORKFLOW_NAME = "apicurio-json-schema-ref-workflow";
    public static final String TASK_NAME = "apicurio-json-schema-ref-workflow";
    public static final String CONNECTOR_NAME = "apicurio-json-schema-ref-workflow";
    private static final String INPUT_PARAMETER = "pilot";
    private static final String INPUT_TOPIC = "apicurio-json-schema-ref";
    private static final String ARTIFACT_ID = INPUT_TOPIC + "-value";
    private static final String VEHICLE_ARTIFACT_ID = "apicurio-json-schema-ref-vehicle";
    private static final String VEHICLE_REF = "https://littlehorse.io/schemas/vehicle.json";

    // Standalone schema referenced by the value schema.
    private static final String VEHICLE_SCHEMA = """
            {
              "$id": "https://littlehorse.io/schemas/vehicle.json",
              "$schema": "http://json-schema.org/draft-07/schema#",
              "type": "object",
              "properties": { "model": { "type": "string" } },
              "required": ["model"]
            }
            """;

    // Value schema referencing the vehicle schema via $ref.
    private static final String PILOT_SCHEMA = """
            {
              "$schema": "http://json-schema.org/draft-07/schema#",
              "type": "object",
              "properties": {
                "pilot": {
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" },
                    "vehicle": { "$ref": "https://littlehorse.io/schemas/vehicle.json" }
                  },
                  "required": ["name", "vehicle"]
                }
              },
              "required": ["pilot"]
            }
            """;

    private static final Workflow WORKFLOW = Workflow.newWorkflow(
            WORKFLOW_NAME, wf -> wf.execute(TASK_NAME, wf.declareJsonObj(INPUT_PARAMETER)));
    private final LittleHorseBlockingStub lhClient = getLittleHorseConfig().getBlockingStub();
    private final ObjectMapper mapper = new ObjectMapper();

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

    @LHTaskMethod(TASK_NAME)
    public String greet(Pilot pilot) {
        String message = String.format(
                "Hello %s flying a %s!", pilot.getName(), pilot.getVehicle().getModel());
        log.info("Executing worker, output: {}", message);
        return message;
    }

    @Test
    public void shouldExecuteWfRunFromReferencedApicurioJsonSchemaRecords() {
        startWorker(this);
        registerWorkflow(WORKFLOW);
        // Register the referenced schema first, then the value schema that references it.
        registerJsonSchema(VEHICLE_ARTIFACT_ID, VEHICLE_SCHEMA);
        registerJsonSchema(
                ARTIFACT_ID,
                PILOT_SCHEMA,
                List.of(new SchemaReference(VEHICLE_REF, "default", VEHICLE_ARTIFACT_ID, "1")));
        createTopics(INPUT_TOPIC);

        produceJsonSchemaValues(
                INPUT_TOPIC,
                pilotEnvelope("Poe", "T-70 X-wing"),
                pilotEnvelope("Rey", "Millennium Falcon"));

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

    private JsonNode pilotEnvelope(String name, String vehicleModel) {
        return mapper.valueToTree(
                Map.of(INPUT_PARAMETER, new Pilot(name, new Vehicle(vehicleModel))));
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
