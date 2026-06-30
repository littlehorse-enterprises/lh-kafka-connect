package e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import e2e.configs.E2ETest;

import io.littlehorse.sdk.common.LHLibUtil;
import io.littlehorse.sdk.common.proto.LHStatus;
import io.littlehorse.sdk.common.proto.LittleHorseGrpc.LittleHorseBlockingStub;
import io.littlehorse.sdk.common.proto.RunWfRequest;
import io.littlehorse.sdk.common.proto.WfRun;
import io.littlehorse.sdk.wfsdk.Workflow;
import io.littlehorse.sdk.worker.LHTaskMethod;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.UUID;

/**
 * Uses the {@code JsonPathMapperTransform} together with the {@code CorrelatedEventSinkConnector} to
 * build the {@code STR} content the correlated event is posted with. The record carries flat
 * {@code {firstName, lastName}} fields and a bare {@code mapping} with the JSONPath {@code concat()}
 * function replaces the whole value with a single string. The record key is the correlation id.
 */
public class CorrelatedEventsJsonPathTest extends E2ETest {

    public static final String WORKFLOW_NAME = "correlated-events-jsonpath";
    public static final String TASK_NAME = "correlated-events-jsonpath";
    public static final String EXTERNAL_EVENT = "correlated-events-jsonpath";
    public static final String CONNECTOR_NAME = "correlated-events-jsonpath";
    public static final String VAR_ID = "id";
    private static final String TOPIC_NAME = "correlated-events-jsonpath";
    private static final Workflow WORKFLOW = Workflow.newWorkflow(
            WORKFLOW_NAME,
            wf -> wf.execute(
                    TASK_NAME,
                    wf.waitForEvent(EXTERNAL_EVENT)
                            .withCorrelationId(wf.declareStr(VAR_ID))
                            .registeredAs(String.class)));
    private final LittleHorseBlockingStub lhClient = getLittleHorseConfig().getBlockingStub();

    @LHTaskMethod(TASK_NAME)
    public String greeting(String fullName) {
        String message = String.format("Hello %s!", fullName);
        log.info("Executing worker, output: {}", message);
        return message;
    }

    @Test
    public void shouldBuildStringCorrelatedEventFromFlatRecord() {
        String wfRunId = UUID.randomUUID().toString();
        String correlatedId = "MyUniqueID." + UUID.randomUUID();

        startWorker(this);
        registerWorkflow(WORKFLOW);
        createTopics(TOPIC_NAME);
        produceValues(
                TOPIC_NAME,
                KafkaMessage.of(
                        correlatedId, "{\"firstName\":\"Luke\",\"lastName\":\"Skywalker\"}"));
        registerConnector(CONNECTOR_NAME, getConnectorConfig());

        lhClient.runWf(RunWfRequest.newBuilder()
                .setId(wfRunId)
                .setWfSpecName(WORKFLOW_NAME)
                .putVariables(VAR_ID, LHLibUtil.objToVarVal(correlatedId))
                .build());

        await(() -> {
            WfRun wfRun = lhClient.getWfRun(LHLibUtil.wfRunIdFromString(wfRunId));
            assertThat(wfRun.getStatus()).isEqualTo(LHStatus.COMPLETED);
        });
    }

    private static HashMap<String, Object> getConnectorConfig() {
        HashMap<String, Object> connectorConfig = new HashMap<>();
        connectorConfig.put("tasks.max", 1);
        connectorConfig.put(
                "connector.class", "io.littlehorse.connect.CorrelatedEventSinkConnector");
        connectorConfig.put("topics", TOPIC_NAME);
        connectorConfig.put("key.converter", "org.apache.kafka.connect.storage.StringConverter");
        connectorConfig.put("value.converter", "org.apache.kafka.connect.json.JsonConverter");
        connectorConfig.put("value.converter.schemas.enable", false);
        connectorConfig.put("transforms", "jsonPath");
        connectorConfig.put(
                "transforms.jsonPath.type",
                "io.littlehorse.connect.transform.JsonPathMapperTransform$Value");
        connectorConfig.put(
                "transforms.jsonPath.mapping",
                "$.concat($.value.firstName, \" \", $.value.lastName)");
        connectorConfig.put("lhc.api.port", 2023);
        connectorConfig.put("lhc.api.host", "littlehorse");
        connectorConfig.put("lhc.tenant.id", "default");
        connectorConfig.put("external.event.name", EXTERNAL_EVENT);
        return connectorConfig;
    }
}
