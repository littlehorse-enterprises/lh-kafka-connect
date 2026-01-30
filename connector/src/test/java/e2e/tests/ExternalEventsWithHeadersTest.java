package e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import e2e.configs.E2ETest;

import io.littlehorse.sdk.common.LHLibUtil;
import io.littlehorse.sdk.common.proto.ExternalEvent;
import io.littlehorse.sdk.common.proto.ExternalEventDefId;
import io.littlehorse.sdk.common.proto.ExternalEventId;
import io.littlehorse.sdk.common.proto.LHStatus;
import io.littlehorse.sdk.common.proto.LittleHorseGrpc.LittleHorseBlockingStub;
import io.littlehorse.sdk.common.proto.RunWfRequest;
import io.littlehorse.sdk.common.proto.WfRun;
import io.littlehorse.sdk.wfsdk.Workflow;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ExternalEventsWithHeadersTest extends E2ETest {

    public static final String WORKFLOW_NAME = "external-events-with-headers";
    private static final String TOPIC_NAME = "external-events-with-headers";
    public static final String EXTERNAL_EVENT = "external-events-with-headers";
    public static final String CONNECTOR_NAME = "external-events-with-headers";
    private static final Workflow WORKFLOW = Workflow.newWorkflow(
            WORKFLOW_NAME, wf -> wf.waitForEvent(EXTERNAL_EVENT).registeredAs(String.class));
    private final LittleHorseBlockingStub lhClient = getLittleHorseConfig().getBlockingStub();

    @Test
    public void shouldSendExternalEventsAfterProducing() {
        String wfRunId = UUID.randomUUID().toString();
        String guid = UUID.randomUUID().toString();
        String content = "my-event-for: " + wfRunId;

        registerWorkflow(WORKFLOW);
        createTopics(TOPIC_NAME);
        produceValues(
                TOPIC_NAME,
                KafkaMessage.of(null, content, Map.of("wfRunId", wfRunId, "guid", guid)));
        registerConnector(CONNECTOR_NAME, getConnectorConfig());

        lhClient.runWf(RunWfRequest.newBuilder()
                .setId(wfRunId)
                .setWfSpecName(WORKFLOW_NAME)
                .build());

        await(() -> {
            WfRun wfRun = lhClient.getWfRun(LHLibUtil.wfRunIdFromString(wfRunId));
            assertThat(wfRun.getStatus()).isEqualTo(LHStatus.COMPLETED);
        });

        await(() -> {
            ExternalEvent externalEvent = lhClient.getExternalEvent(ExternalEventId.newBuilder()
                    .setExternalEventDefId(ExternalEventDefId.newBuilder()
                            .setName(EXTERNAL_EVENT)
                            .build())
                    .setGuid(guid)
                    .setWfRunId(LHLibUtil.wfRunIdFromString(wfRunId))
                    .build());
            assertThat(externalEvent.getClaimed()).isTrue();
            assertThat(externalEvent.getContent().getStr()).isEqualTo(content);
        });
    }

    private static HashMap<String, Object> getConnectorConfig() {
        HashMap<String, Object> connectorConfig = new HashMap<>();
        connectorConfig.put("tasks.max", 1);
        connectorConfig.put("connector.class", "io.littlehorse.connect.ExternalEventSinkConnector");
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
