package e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import e2e.configs.E2ETest;

import io.littlehorse.sdk.common.LHLibUtil;
import io.littlehorse.sdk.common.proto.LHStatus;
import io.littlehorse.sdk.common.proto.LittleHorseGrpc.LittleHorseBlockingStub;
import io.littlehorse.sdk.common.proto.SearchWfRunRequest;
import io.littlehorse.sdk.common.proto.WfRun;
import io.littlehorse.sdk.common.proto.WfRunId;
import io.littlehorse.sdk.common.proto.WfRunIdList;
import io.littlehorse.sdk.wfsdk.Workflow;
import io.littlehorse.sdk.worker.LHTaskMethod;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;

public class RunWorkflowsAutoIdempotencyKeyDisabledTest extends E2ETest {

    public static final String WORKFLOW_NAME = "wfrun-auto-idempotency-disabled";
    public static final String TASK_NAME = "wfrun-auto-idempotency-disabled";
    public static final String CONNECTOR_NAME = "wfrun-auto-idempotency-disabled";
    private static final String INPUT_PARAMETER = "name";
    private static final String INPUT_TOPIC = "wfrun-auto-idempotency-disabled";
    private static final String DLQ_TOPIC = "wfrun-auto-idempotency-disabled-errors";
    private static final String WF_RUN_ID = "luke-wf-run-id";
    private static final Workflow WORKFLOW = Workflow.newWorkflow(
            WORKFLOW_NAME, wf -> wf.execute(TASK_NAME, wf.declareStr(INPUT_PARAMETER)));
    private final LittleHorseBlockingStub lhClient = getLittleHorseConfig().getBlockingStub();

    @LHTaskMethod(TASK_NAME)
    public String greet(String name) {
        String message = String.format("Hello %s!", name);
        log.info("Executing worker, output: {}", message);
        return message;
    }

    @Test
    public void shouldNotGenerateIdempotencyKeyWhenDisabled() {
        startWorker(this);
        registerWorkflow(WORKFLOW);
        createTopics(INPUT_TOPIC, DLQ_TOPIC);
        // offset 0 has no key nor wfRunId header: with auto idempotency key generation disabled it
        // must fail with a non-retriable error and be routed to the DLQ instead of falling back to
        // a generated id. offset 1 provides an explicit WfRunId via the record key, so it must run
        // normally.
        produceValues(
                INPUT_TOPIC, KafkaMessage.of(null, "Anakin"), KafkaMessage.of(WF_RUN_ID, "Luke"));
        registerConnector(CONNECTOR_NAME, getConnectorConfig());

        await(() -> {
            WfRun wfRun = lhClient.getWfRun(LHLibUtil.wfRunIdFromString(WF_RUN_ID));
            assertThat(wfRun.getStatus()).isEqualTo(LHStatus.COMPLETED);
        });

        await(() -> {
            List<ConsumerRecord<byte[], byte[]>> dlqRecords =
                    consumeRecords(DLQ_TOPIC, Duration.ofSeconds(1));
            assertThat(dlqRecords).isNotEmpty();
        });

        // only the record with an explicit WfRunId produced a WfRun; the keyless record did not
        // generate an idempotency-key-based id
        await(() -> {
            SearchWfRunRequest criteria =
                    SearchWfRunRequest.newBuilder().setWfSpecName(WORKFLOW_NAME).build();
            WfRunIdList result = lhClient.searchWfRun(criteria);
            assertThat(result.getResultsList())
                    .containsExactly(WfRunId.newBuilder().setId(WF_RUN_ID).build());
        });
    }

    private static HashMap<String, Object> getConnectorConfig() {
        HashMap<String, Object> connectorConfig = new HashMap<>();
        connectorConfig.put("tasks.max", 1);
        connectorConfig.put("connector.class", "io.littlehorse.connect.WfRunSinkConnector");
        connectorConfig.put("topics", INPUT_TOPIC);
        connectorConfig.put("key.converter", "org.apache.kafka.connect.storage.StringConverter");
        connectorConfig.put("value.converter", "org.apache.kafka.connect.storage.StringConverter");
        connectorConfig.put("transforms", "HoistField");
        connectorConfig.put(
                "transforms.HoistField.type",
                "org.apache.kafka.connect.transforms.HoistField$Value");
        connectorConfig.put("transforms.HoistField.field", INPUT_PARAMETER);
        connectorConfig.put("errors.tolerance", "all");
        connectorConfig.put("errors.deadletterqueue.topic.name", DLQ_TOPIC);
        connectorConfig.put("errors.deadletterqueue.topic.replication.factor", 1);
        connectorConfig.put("errors.deadletterqueue.context.headers.enable", true);
        connectorConfig.put("auto.idempotency.key.enabled", false);
        connectorConfig.put("lhc.api.port", 2023);
        connectorConfig.put("lhc.api.host", "littlehorse");
        connectorConfig.put("lhc.tenant.id", "default");
        connectorConfig.put("wf.spec.name", WORKFLOW_NAME);
        return connectorConfig;
    }
}
