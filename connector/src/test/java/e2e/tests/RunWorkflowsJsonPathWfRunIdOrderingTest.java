package e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import e2e.configs.E2ETest;

import io.littlehorse.sdk.wfsdk.Workflow;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;

/**
 * Verifies the ordering guard for value-derived {@code wfRunId}s. The connector chains a {@code
 * $Value} mapper and a {@code $Headers} id mapper, but they are listed in the wrong order: the
 * {@code $Value} transform runs first and rebuilds the record value from scratch, dropping the
 * {@code orderId} field. When the {@code $Headers} id transform then evaluates {@code
 * $.concat("order-", $.value.orderId)} it reads {@code null}, producing the malformed id {@code
 * "order-"}. The connector's guard rejects it with a clear {@code DataException} instead of the
 * opaque gRPC {@code 'id' must be a valid hostname}, and with {@code errors.tolerance=all} the
 * record is routed to the Dead Letter Queue.
 */
public class RunWorkflowsJsonPathWfRunIdOrderingTest extends E2ETest {

    public static final String WORKFLOW_NAME = "jsonpath-wfrunid-ordering-workflow";
    public static final String CONNECTOR_NAME = "jsonpath-wfrunid-ordering-workflow";
    private static final String INPUT_TOPIC = "jsonpath-wfrunid-ordering";
    private static final String DLQ_TOPIC = "jsonpath-wfrunid-ordering-errors";
    private static final Workflow WORKFLOW = Workflow.newWorkflow(WORKFLOW_NAME, wf -> {
        wf.declareStr("customer");
        wf.sleepSeconds(1);
    });

    @Test
    public void shouldRouteMisorderedWfRunIdRecordToDlq() {
        registerWorkflow(WORKFLOW);
        createTopics(INPUT_TOPIC, DLQ_TOPIC);
        produceValues(
                INPUT_TOPIC, KafkaMessage.of("{\"orderId\":\"abc123\",\"customer\":\"Han Solo\"}"));
        registerConnector(CONNECTOR_NAME, getConnectorConfig());

        await(() -> {
            List<ConsumerRecord<byte[], byte[]>> dlqRecords =
                    consumeRecords(DLQ_TOPIC, Duration.ofSeconds(1));
            assertThat(dlqRecords).isNotEmpty();
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
        // Wrong order on purpose: the $Value transform runs before the $Headers id transform.
        connectorConfig.put("transforms", "wfRunVariables,wfRunId");
        connectorConfig.put(
                "transforms.wfRunVariables.type",
                "io.littlehorse.connect.transform.JsonPathMapperTransform$Value");
        connectorConfig.put("transforms.wfRunVariables.mapping.customer", "$.value.customer");
        connectorConfig.put(
                "transforms.wfRunId.type",
                "io.littlehorse.connect.transform.JsonPathMapperTransform$Headers");
        connectorConfig.put(
                "transforms.wfRunId.mapping.wfRunId", "$.concat(\"order-\", $.value.orderId)");
        connectorConfig.put("errors.tolerance", "all");
        connectorConfig.put("errors.deadletterqueue.topic.name", DLQ_TOPIC);
        connectorConfig.put("errors.deadletterqueue.topic.replication.factor", 1);
        connectorConfig.put("errors.deadletterqueue.context.headers.enable", true);
        connectorConfig.put("lhc.api.port", 2023);
        connectorConfig.put("lhc.api.host", "littlehorse");
        connectorConfig.put("lhc.tenant.id", "default");
        connectorConfig.put("wf.spec.name", WORKFLOW_NAME);
        return connectorConfig;
    }
}
