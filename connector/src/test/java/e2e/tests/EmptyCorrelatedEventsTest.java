package e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import e2e.configs.E2ETest;

import io.littlehorse.sdk.common.LHLibUtil;
import io.littlehorse.sdk.common.proto.LHStatus;
import io.littlehorse.sdk.common.proto.LittleHorseGrpc.LittleHorseBlockingStub;
import io.littlehorse.sdk.common.proto.RunWfRequest;
import io.littlehorse.sdk.common.proto.WfRun;
import io.littlehorse.sdk.wfsdk.Workflow;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.UUID;

public class EmptyCorrelatedEventsTest extends E2ETest {

    public static final String WORKFLOW_NAME = "empty-correlated-events";
    private static final String TOPIC_NAME = "empty-correlated-events";
    public static final String EXTERNAL_EVENT = "empty-correlated-events";
    public static final String CONNECTOR_NAME = "empty-correlated-events";
    public static final String VAR_ID = "id";
    private static final Workflow WORKFLOW =
            Workflow.newWorkflow(WORKFLOW_NAME, wf -> wf.waitForEvent(EXTERNAL_EVENT)
                    .registeredAs(null)
                    .withCorrelationId(wf.declareStr(VAR_ID)));
    private final LittleHorseBlockingStub lhClient = getLittleHorseConfig().getBlockingStub();

    @Test
    public void shouldSendCorrelatedEventsAfterProducing() {
        String wfRunId = UUID.randomUUID().toString();
        String correlatedId = "MyUniqueID." + UUID.randomUUID();

        registerWorkflow(WORKFLOW);
        createTopics(TOPIC_NAME);
        produceValues(TOPIC_NAME, KafkaMessage.of(correlatedId, null));
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
        connectorConfig.put("value.converter", "org.apache.kafka.connect.storage.StringConverter");
        connectorConfig.put("lhc.api.port", 2023);
        connectorConfig.put("lhc.api.host", "littlehorse");
        connectorConfig.put("lhc.tenant.id", "default");
        connectorConfig.put("external.event.name", EXTERNAL_EVENT);
        return connectorConfig;
    }
}
