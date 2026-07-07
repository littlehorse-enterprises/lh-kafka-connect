package e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
import java.util.Map;

/**
 * Verifies that a record which cannot be deserialized by the {@code JsonSchemaKafkaConverter} (for
 * example, a message that is not framed as an Apicurio JSON Schema payload and therefore does not
 * match a valid schema) fails conversion and is routed to the Dead Letter Queue by Kafka Connect,
 * while a valid record that follows it is still processed into a {@code WfRun}.
 */
public class RunWorkflowsApicurioJsonSchemaDlqTest extends E2ETest {

    public static final String WORKFLOW_NAME = "apicurio-json-schema-dlq";
    public static final String TASK_NAME = "apicurio-json-schema-dlq";
    public static final String CONNECTOR_NAME = "apicurio-json-schema-dlq";
    private static final String INPUT_PARAMETER = "person";
    private static final String INPUT_TOPIC = "apicurio-json-schema-dlq";
    private static final String DLQ_TOPIC = "apicurio-json-schema-dlq-errors";
    private static final String ARTIFACT_ID = INPUT_TOPIC + "-value";
    private static final String JSON_SCHEMA =
            "{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"object\"}";

    private static final Workflow WORKFLOW = Workflow.newWorkflow(
            WORKFLOW_NAME, wf -> wf.execute(TASK_NAME, wf.declareJsonObj(INPUT_PARAMETER)));
    private final LittleHorseBlockingStub lhClient = getLittleHorseConfig().getBlockingStub();
    private final ObjectMapper mapper = new ObjectMapper();

    public static class Person {
        private String firstName;
        private String secondName;

        public Person() {}

        public Person(String firstName, String secondName) {
            this.firstName = firstName;
            this.secondName = secondName;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getSecondName() {
            return secondName;
        }

        public void setSecondName(String secondName) {
            this.secondName = secondName;
        }

        @Override
        public String toString() {
            return String.format("%s %s", firstName, secondName);
        }
    }

    @LHTaskMethod(TASK_NAME)
    public String greet(Person person) {
        String message = String.format("Hello %s!", person);
        log.info("Executing worker, output: {}", message);
        return message;
    }

    @Test
    public void shouldRouteInvalidSchemaRecordToDlqAndKeepProcessing() {
        startWorker(this);
        registerWorkflow(WORKFLOW);
        registerJsonSchema(ARTIFACT_ID, JSON_SCHEMA);
        createTopics(INPUT_TOPIC, DLQ_TOPIC);

        // offset 0: not an Apicurio JSON Schema payload -> the converter fails to deserialize it.
        // With errors.tolerance=all, Kafka Connect routes the conversion failure to the DLQ.
        produceValues(
                INPUT_TOPIC, KafkaMessage.of("this is not a valid apicurio json schema message"));
        // offset 1: a valid Apicurio JSON Schema record -> processed into a WfRun.
        produceJsonSchemaValues(INPUT_TOPIC, personEnvelope("Leia", "Organa"));

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

    private JsonNode personEnvelope(String firstName, String secondName) {
        return mapper.valueToTree(Map.of(INPUT_PARAMETER, new Person(firstName, secondName)));
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
