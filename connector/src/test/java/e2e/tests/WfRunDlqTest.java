package e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import e2e.configs.E2ETest;

import io.littlehorse.sdk.common.LHLibUtil;
import io.littlehorse.sdk.common.proto.LHStatus;
import io.littlehorse.sdk.common.proto.LittleHorseGrpc.LittleHorseBlockingStub;
import io.littlehorse.sdk.common.proto.WfRun;
import io.littlehorse.sdk.wfsdk.Workflow;
import io.littlehorse.sdk.worker.LHTaskMethod;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;

public class WfRunDlqTest extends E2ETest {

    public static final String WORKFLOW_NAME = "wfrun-dlq";
    public static final String TASK_NAME = "wfrun-dlq";
    public static final String CONNECTOR_NAME = "wfrun-dlq";
    private static final String INPUT_PARAMETER = "name";
    private static final String INPUT_TOPIC = "wfrun-dlq";
    private static final String DLQ_TOPIC = "wfrun-dlq-errors";
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
    public void shouldRouteBadRecordToDlqAndKeepProcessing() {
        startWorker(this);
        registerWorkflow(WORKFLOW);
        createTopics(INPUT_TOPIC, DLQ_TOPIC);
        // offset 0 is a permanent error (not a key-value pair); offset 1 is a valid record.
        // With errors.tolerance=all the bad record must be routed to the DLQ without blocking
        // the valid record that follows it.
        produceValues(
                INPUT_TOPIC,
                KafkaMessage.of("42"),
                KafkaMessage.of(getJsonStr(new NameInput("Luke"))));
        registerConnector(CONNECTOR_NAME, getConnectorConfig());

        await(() -> {
            WfRun wfRun = lhClient.getWfRun(LHLibUtil.wfRunIdFromString(
                    "%s-%s-0-1".formatted(CONNECTOR_NAME, INPUT_TOPIC)));
            assertThat(wfRun.getStatus()).isEqualTo(LHStatus.COMPLETED);
        });

        await(() -> {
            List<ConsumerRecord<byte[], byte[]>> dlqRecords =
                    consumeRecords(DLQ_TOPIC, Duration.ofSeconds(1));
            assertThat(dlqRecords).isNotEmpty();
        });
    }

    private String getJsonStr(Object object) {
        return LHLibUtil.serializeToJson(object).getJsonStr();
    }

    public static class NameInput {

        private String name;

        public NameInput() {}

        public NameInput(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    private static HashMap<String, Object> getConnectorConfig() {
        HashMap<String, Object> connectorConfig = new HashMap<>();
        connectorConfig.put("tasks.max", 1);
        connectorConfig.put("connector.class", "io.littlehorse.connect.WfRunSinkConnector");
        connectorConfig.put("topics", INPUT_TOPIC);
        connectorConfig.put("key.converter", "org.apache.kafka.connect.storage.StringConverter");
        connectorConfig.put("value.converter", "org.apache.kafka.connect.json.JsonConverter");
        connectorConfig.put("value.converter.schemas.enable", false);
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
