package e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import e2e.configs.E2ETest;

import io.littlehorse.sdk.common.LHLibUtil;
import io.littlehorse.sdk.common.proto.LHStatus;
import io.littlehorse.sdk.common.proto.LittleHorseGrpc.LittleHorseBlockingStub;
import io.littlehorse.sdk.common.proto.RunWfRequest;
import io.littlehorse.sdk.common.proto.WfRun;
import io.littlehorse.sdk.wfsdk.Workflow;
import io.littlehorse.sdk.worker.LHStructDef;
import io.littlehorse.sdk.worker.LHTaskMethod;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.UUID;

/**
 * Uses the {@code JsonPathMapperTransform} together with the {@code ExternalEventSinkConnector} to
 * reshape a flat record value into the nested {@code STRUCT} content the external event is posted
 * with. The record carries flat {@code {name, model}} fields and the mappings
 * {@code mapping.name = $.value.name} and {@code mapping.vehicle.model = $.value.model} build the
 * two-level {@code Pilot} struct the connector detects from the {@code ExternalEventDef}.
 */
public class ExternalEventsJsonPathStructTest extends E2ETest {

    public static final String WORKFLOW_NAME = "external-events-jsonpath-struct";
    public static final String TASK_NAME = "external-events-jsonpath-struct";
    public static final String EXTERNAL_EVENT = "external-events-jsonpath-struct";
    public static final String CONNECTOR_NAME = "external-events-jsonpath-struct";
    private static final String TOPIC_NAME = "external-events-jsonpath-struct";
    private static final Workflow WORKFLOW = Workflow.newWorkflow(
            WORKFLOW_NAME,
            wf -> wf.execute(TASK_NAME, wf.waitForEvent(EXTERNAL_EVENT).registeredAs(Pilot.class)));
    private final LittleHorseBlockingStub lhClient = getLittleHorseConfig().getBlockingStub();

    @LHStructDef("external-events-jsonpath-struct-vehicle")
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

    @LHStructDef("external-events-jsonpath-struct-pilot")
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
    public String greeting(Pilot pilot) {
        String message = String.format(
                "Hello %s, you're driving a %s!",
                pilot.getName(), pilot.getVehicle().getModel());
        log.info("Executing worker, output: {}", message);
        return message;
    }

    @Test
    public void shouldReshapeFlatRecordIntoStructExternalEvent() {
        String wfRunId = UUID.randomUUID().toString();

        registerStructDef(this, Vehicle.class, Pilot.class);
        startWorker(this);
        registerWorkflow(WORKFLOW);
        createTopics(TOPIC_NAME);
        produceValues(
                TOPIC_NAME, KafkaMessage.of(wfRunId, "{\"name\":\"Luke\",\"model\":\"X-wing\"}"));
        registerConnector(CONNECTOR_NAME, getConnectorConfig());

        lhClient.runWf(RunWfRequest.newBuilder()
                .setId(wfRunId)
                .setWfSpecName(WORKFLOW_NAME)
                .build());

        await(() -> {
            WfRun wfRun = lhClient.getWfRun(LHLibUtil.wfRunIdFromString(wfRunId));
            assertThat(wfRun.getStatus()).isEqualTo(LHStatus.COMPLETED);
        });
    }

    private static HashMap<String, Object> getConnectorConfig() {
        HashMap<String, Object> connectorConfig = new HashMap<>();
        connectorConfig.put("tasks.max", 1);
        connectorConfig.put("connector.class", "io.littlehorse.connect.ExternalEventSinkConnector");
        connectorConfig.put("topics", TOPIC_NAME);
        connectorConfig.put("key.converter", "org.apache.kafka.connect.storage.StringConverter");
        connectorConfig.put("value.converter", "org.apache.kafka.connect.json.JsonConverter");
        connectorConfig.put("value.converter.schemas.enable", false);
        connectorConfig.put("transforms", "jsonPath");
        connectorConfig.put(
                "transforms.jsonPath.type",
                "io.littlehorse.connect.transform.JsonPathMapperTransform$Value");
        connectorConfig.put("transforms.jsonPath.mapping.name", "$.value.name");
        connectorConfig.put("transforms.jsonPath.mapping.vehicle.model", "$.value.model");
        connectorConfig.put("lhc.api.port", 2023);
        connectorConfig.put("lhc.api.host", "littlehorse");
        connectorConfig.put("lhc.tenant.id", "default");
        connectorConfig.put("external.event.name", EXTERNAL_EVENT);
        return connectorConfig;
    }
}
