package e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import e2e.configs.E2ETest;

import io.littlehorse.sdk.common.LHLibUtil;
import io.littlehorse.sdk.common.proto.ExternalEvent;
import io.littlehorse.sdk.common.proto.ExternalEventDefId;
import io.littlehorse.sdk.common.proto.ExternalEventId;
import io.littlehorse.sdk.common.proto.LittleHorseGrpc.LittleHorseBlockingStub;
import io.littlehorse.sdk.common.proto.RunWfRequest;
import io.littlehorse.sdk.wfsdk.Workflow;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ExternalEventsAutoIdempotencyKeyDisabledTest extends E2ETest {

    public static final String WORKFLOW_NAME = "external-events-auto-idempotency-disabled";
    private static final String TOPIC_NAME = "external-events-auto-idempotency-disabled";
    private static final String DLQ_TOPIC = "external-events-auto-idempotency-disabled-errors";
    public static final String EXTERNAL_EVENT = "external-events-auto-idempotency-disabled";
    public static final String CONNECTOR_NAME = "external-events-auto-idempotency-disabled";
    private static final Workflow WORKFLOW = Workflow.newWorkflow(
            WORKFLOW_NAME, wf -> wf.waitForEvent(EXTERNAL_EVENT).registeredAs(String.class));
    private final LittleHorseBlockingStub lhClient = getLittleHorseConfig().getBlockingStub();

    @Test
    public void shouldNotGenerateGuidWhenDisabled() {
        String wfRunId = UUID.randomUUID().toString();
        String guid = UUID.randomUUID().toString();
        String content = "my-event-for: " + wfRunId;

        registerWorkflow(WORKFLOW);
        createTopics(TOPIC_NAME, DLQ_TOPIC);
        // offset 0 has a wfRunId but no guid header: with auto idempotency key generation disabled
        // it must fail with a non-retriable error and be routed to the DLQ instead of falling back
        // to a generated guid. offset 1 provides an explicit guid, so the external event must be
        // posted normally.
        produceValues(
                TOPIC_NAME,
                KafkaMessage.of(null, content, Map.of("wfRunId", wfRunId)),
                KafkaMessage.of(null, content, Map.of("wfRunId", wfRunId, "guid", guid)));
        registerConnector(CONNECTOR_NAME, getConnectorConfig());

        lhClient.runWf(RunWfRequest.newBuilder()
                .setId(wfRunId)
                .setWfSpecName(WORKFLOW_NAME)
                .build());

        await(() -> {
            ExternalEvent externalEvent = lhClient.getExternalEvent(ExternalEventId.newBuilder()
                    .setExternalEventDefId(ExternalEventDefId.newBuilder()
                            .setName(EXTERNAL_EVENT)
                            .build())
                    .setGuid(guid)
                    .setWfRunId(LHLibUtil.wfRunIdFromString(wfRunId))
                    .build());
            assertThat(externalEvent.getContent().getStr()).isEqualTo(content);
        });

        await(() -> {
            List<ConsumerRecord<byte[], byte[]>> dlqRecords =
                    consumeRecords(DLQ_TOPIC, Duration.ofSeconds(1));
            assertThat(dlqRecords).isNotEmpty();
        });
    }

    private static HashMap<String, Object> getConnectorConfig() {
        HashMap<String, Object> connectorConfig = new HashMap<>();
        connectorConfig.put("tasks.max", 1);
        connectorConfig.put("connector.class", "io.littlehorse.connect.ExternalEventSinkConnector");
        connectorConfig.put("topics", TOPIC_NAME);
        connectorConfig.put("key.converter", "org.apache.kafka.connect.storage.StringConverter");
        connectorConfig.put("value.converter", "org.apache.kafka.connect.storage.StringConverter");
        connectorConfig.put("errors.tolerance", "all");
        connectorConfig.put("errors.deadletterqueue.topic.name", DLQ_TOPIC);
        connectorConfig.put("errors.deadletterqueue.topic.replication.factor", 1);
        connectorConfig.put("errors.deadletterqueue.context.headers.enable", true);
        connectorConfig.put("auto.idempotency.key.enabled", false);
        connectorConfig.put("lhc.api.port", 2023);
        connectorConfig.put("lhc.api.host", "littlehorse");
        connectorConfig.put("lhc.tenant.id", "default");
        connectorConfig.put("external.event.name", EXTERNAL_EVENT);
        return connectorConfig;
    }
}
