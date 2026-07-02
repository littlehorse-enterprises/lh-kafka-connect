package e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import e2e.configs.E2ETest;

import io.littlehorse.sdk.common.LHLibUtil;
import io.littlehorse.sdk.common.proto.LHStatus;
import io.littlehorse.sdk.common.proto.LittleHorseGrpc.LittleHorseBlockingStub;
import io.littlehorse.sdk.common.proto.SearchWfRunRequest;
import io.littlehorse.sdk.common.proto.Variable;
import io.littlehorse.sdk.common.proto.VariableId;
import io.littlehorse.sdk.common.proto.VariableValue;
import io.littlehorse.sdk.common.proto.WfRun;
import io.littlehorse.sdk.common.proto.WfRunId;
import io.littlehorse.sdk.common.proto.WfRunIdList;
import io.littlehorse.sdk.wfsdk.Workflow;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;

/**
 * Uses the {@code JsonPathMapperTransform} together with the {@code WfRunSinkConnector} to explore
 * what happens when a WfSpec input variable is fed from a JSONPath that is absent in the source
 * record. A missing JSONPath leaf resolves to {@code null}, which the connector turns into an unset
 * variable value that is still present in the {@code RunWf} request. Because the variable is
 * present (as unset), the server's required-variable check is satisfied, so the {@code WfRun}
 * starts whether the target variable is optional or required, leaving the variable unset. A
 * required variable only fails the {@code WfRun} when it is not mapped at all (truly absent), in
 * which case the record is routed to the DLQ.
 */
public class RunWorkflowsJsonPathMissingFieldTest extends E2ETest {

    private final LittleHorseBlockingStub lhClient = getLittleHorseConfig().getBlockingStub();

    @Test
    public void shouldLeaveOptionalVariableUnsetWhenPathMissing() {
        String workflowName = "jsonpath-missing-optional";
        String connectorName = "jsonpath-missing-optional";
        String inputTopic = "jsonpath-missing-optional";
        Workflow workflow = Workflow.newWorkflow(workflowName, wf -> {
            wf.declareStr("present");
            wf.declareStr("missing");
            wf.sleepSeconds(1);
        });

        registerWorkflow(workflow);
        createTopics(inputTopic);
        produceValues(inputTopic, KafkaMessage.of("{\"present\":\"here\"}"));
        registerConnector(connectorName, getConnectorConfig(workflowName, inputTopic, "missing"));

        WfRunId wfRunId = WfRunId.newBuilder()
                .setId("%s-%s-0-0".formatted(connectorName, inputTopic))
                .build();

        await(() -> {
            SearchWfRunRequest criteria = SearchWfRunRequest.newBuilder()
                    .setStatus(LHStatus.COMPLETED)
                    .setWfSpecName(workflowName)
                    .build();
            WfRunIdList result = lhClient.searchWfRun(criteria);
            assertThat(result.getResultsList()).contains(wfRunId);
        });

        // The existing field is mapped as usual.
        Variable present = lhClient.getVariable(VariableId.newBuilder()
                .setWfRunId(wfRunId)
                .setThreadRunNumber(0)
                .setName("present")
                .build());
        assertThat(present.getValue().getStr()).isEqualTo("here");

        // The missing path resolves to null, leaving the optional variable unset.
        Variable missing = lhClient.getVariable(VariableId.newBuilder()
                .setWfRunId(wfRunId)
                .setThreadRunNumber(0)
                .setName("missing")
                .build());
        assertThat(missing.getValue().getValueCase())
                .isEqualTo(VariableValue.ValueCase.VALUE_NOT_SET);
    }

    @Test
    public void shouldStartRequiredVariableUnsetWhenPathMissing() {
        String workflowName = "jsonpath-missing-required-mapped";
        String connectorName = "jsonpath-missing-required-mapped";
        String inputTopic = "jsonpath-missing-required-mapped";
        Workflow workflow = Workflow.newWorkflow(workflowName, wf -> {
            wf.declareStr("present");
            wf.declareStr("mandatory").required();
            wf.sleepSeconds(1);
        });

        registerWorkflow(workflow);
        createTopics(inputTopic);
        produceValues(inputTopic, KafkaMessage.of("{\"present\":\"here\"}"));
        registerConnector(connectorName, getConnectorConfig(workflowName, inputTopic, "mandatory"));

        WfRunId wfRunId = WfRunId.newBuilder()
                .setId("%s-%s-0-0".formatted(connectorName, inputTopic))
                .build();

        // The required variable is mapped, so the missing path still supplies it (as an unset
        // value). The server's required check is satisfied by the variable's presence, so the
        // WfRun starts and completes.
        await(() -> {
            WfRun wfRun = lhClient.getWfRun(LHLibUtil.wfRunIdFromString(wfRunId.getId()));
            assertThat(wfRun.getStatus()).isEqualTo(LHStatus.COMPLETED);
        });

        Variable mandatory = lhClient.getVariable(VariableId.newBuilder()
                .setWfRunId(wfRunId)
                .setThreadRunNumber(0)
                .setName("mandatory")
                .build());
        assertThat(mandatory.getValue().getValueCase())
                .isEqualTo(VariableValue.ValueCase.VALUE_NOT_SET);
    }

    @Test
    public void shouldRouteToDlqWhenRequiredVariableNotMapped() {
        String workflowName = "jsonpath-missing-required-unmapped";
        String connectorName = "jsonpath-missing-required-unmapped";
        String inputTopic = "jsonpath-missing-required-unmapped";
        String dlqTopic = "jsonpath-missing-required-unmapped-errors";
        Workflow workflow = Workflow.newWorkflow(workflowName, wf -> {
            wf.declareStr("present");
            wf.declareStr("mandatory").required();
            wf.sleepSeconds(1);
        });

        registerWorkflow(workflow);
        createTopics(inputTopic, dlqTopic);
        produceValues(inputTopic, KafkaMessage.of("{\"present\":\"here\"}"));

        // The required variable is not mapped at all, so it is absent from the RunWf request. The
        // server rejects the request (a permanent error), which errors.tolerance=all routes to the
        // DLQ instead of failing the task.
        HashMap<String, Object> config = getConnectorConfig(workflowName, inputTopic, null);
        config.put("errors.tolerance", "all");
        config.put("errors.deadletterqueue.topic.name", dlqTopic);
        config.put("errors.deadletterqueue.topic.replication.factor", 1);
        config.put("errors.deadletterqueue.context.headers.enable", true);
        registerConnector(connectorName, config);

        await(() -> {
            List<ConsumerRecord<byte[], byte[]>> dlqRecords =
                    consumeRecords(dlqTopic, Duration.ofSeconds(1));
            assertThat(dlqRecords).isNotEmpty();
        });
    }

    private static HashMap<String, Object> getConnectorConfig(
            String workflowName, String topic, String missingVar) {
        HashMap<String, Object> connectorConfig = new HashMap<>();
        connectorConfig.put("tasks.max", 1);
        connectorConfig.put("connector.class", "io.littlehorse.connect.WfRunSinkConnector");
        connectorConfig.put("topics", topic);
        connectorConfig.put("key.converter", "org.apache.kafka.connect.storage.StringConverter");
        connectorConfig.put("value.converter", "org.apache.kafka.connect.json.JsonConverter");
        connectorConfig.put("value.converter.schemas.enable", false);
        connectorConfig.put("transforms", "jsonPath");
        connectorConfig.put(
                "transforms.jsonPath.type",
                "io.littlehorse.connect.transform.JsonPathMapperTransform$Value");
        connectorConfig.put("transforms.jsonPath.mapping.present", "$.value.present");
        if (missingVar != null) {
            // The source record has no field for missingVar; the JSONPath resolves to null.
            connectorConfig.put(
                    "transforms.jsonPath.mapping." + missingVar, "$.value." + missingVar);
        }
        connectorConfig.put("lhc.api.port", 2023);
        connectorConfig.put("lhc.api.host", "littlehorse");
        connectorConfig.put("lhc.tenant.id", "default");
        connectorConfig.put("wf.spec.name", workflowName);
        return connectorConfig;
    }
}
