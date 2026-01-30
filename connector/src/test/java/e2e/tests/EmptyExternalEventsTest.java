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

public class EmptyExternalEventsTest extends E2ETest {

    public static final String WORKFLOW_NAME = "empty-external-events";
    private static final String TOPIC_NAME = "empty-external-events";
    public static final String EXTERNAL_EVENT = "empty-external-events";
    private static final Workflow WORKFLOW = Workflow.newWorkflow(
            WORKFLOW_NAME, wf -> wf.waitForEvent(EXTERNAL_EVENT).registeredAs(null));
    public static final String CONNECTOR_NAME = "empty-external-events";
    private final LittleHorseBlockingStub lhClient = getLittleHorseConfig().getBlockingStub();

    @Test
    public void shouldSendExternalEventsAfterProducing() {
        String wfRunId = UUID.randomUUID().toString();

        registerWorkflow(WORKFLOW);
        createTopics(TOPIC_NAME);
        produceValues(TOPIC_NAME, KafkaMessage.of(wfRunId, null));
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
        connectorConfig.put("lhc.api.port", 2023);
        connectorConfig.put("lhc.api.host", "littlehorse");
        connectorConfig.put("lhc.tenant.id", "default");
        connectorConfig.put("external.event.name", EXTERNAL_EVENT);
        return connectorConfig;
    }
}
