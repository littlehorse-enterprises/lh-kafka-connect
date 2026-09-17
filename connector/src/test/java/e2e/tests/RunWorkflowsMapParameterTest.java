package e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import e2e.configs.E2ETest;

import io.littlehorse.sdk.common.proto.LHStatus;
import io.littlehorse.sdk.common.proto.LittleHorseGrpc.LittleHorseBlockingStub;
import io.littlehorse.sdk.common.proto.SearchWfRunRequest;
import io.littlehorse.sdk.common.proto.WfRunIdList;
import io.littlehorse.sdk.wfsdk.Workflow;
import io.littlehorse.sdk.worker.LHStructDef;
import io.littlehorse.sdk.worker.LHTaskMethod;
import io.littlehorse.sdk.worker.LHType;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class RunWorkflowsMapParameterTest extends E2ETest {

    private static final String WORKFLOW_NAME = "map-workflow";
    private static final String TASK_NAME = "map-workflow";
    private static final String CONNECTOR_NAME = "map-workflow";
    private static final String INPUT_PARAMETER = "lookup";
    private static final String INPUT_TOPIC = "map-input";
    private static final String STRUCT_WORKFLOW_NAME = "map-struct-workflow";
    private static final String STRUCT_TASK_NAME = "map-struct-workflow";
    private static final String STRUCT_CONNECTOR_NAME = "map-struct-workflow";
    private static final String STRUCT_INPUT_TOPIC = "map-struct-input";
    private static final Workflow WORKFLOW = Workflow.newWorkflow(
            WORKFLOW_NAME,
            wf -> wf.execute(
                    TASK_NAME, wf.declareMap(INPUT_PARAMETER, Integer.class, String.class)));
    private static final Workflow STRUCT_WORKFLOW = Workflow.newWorkflow(
            STRUCT_WORKFLOW_NAME,
            wf -> wf.execute(
                    STRUCT_TASK_NAME, wf.declareMap(INPUT_PARAMETER, Integer.class, Pilot.class)));
    private final LittleHorseBlockingStub lhClient = getLittleHorseConfig().getBlockingStub();

    @LHStructDef("map-workflow-pilot")
    public static class Pilot {

        private String name;

        public Pilot() {}

        public Pilot(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @LHTaskMethod(TASK_NAME)
    public String mapWorkflow(@LHType(isLHMap = true) Map<Integer, String> lookup) {
        String result = lookup.get(1);
        if (!"one".equals(result)) {
            throw new IllegalArgumentException(
                    "Expected the JSON object key to be converted to INT");
        }
        return result;
    }

    @LHTaskMethod(STRUCT_TASK_NAME)
    public String mapStructWorkflow(@LHType(isLHMap = true) Map<Integer, Pilot> lookup) {
        Pilot result = lookup.get(1);
        if (result == null || !"Luke".equals(result.getName())) {
            throw new IllegalArgumentException(
                    "Expected the JSON object to be converted to a map with INT keys and STRUCT values");
        }
        return result.getName();
    }

    @Test
    public void shouldExecuteWfRunWithNativeMapAfterProducing() {
        registerStructDef(this, Pilot.class);
        startWorker(this);
        registerWorkflow(WORKFLOW);
        createTopics(INPUT_TOPIC);
        produceValues(INPUT_TOPIC, KafkaMessage.of("{\"lookup\":{\"1\":\"one\",\"2\":\"two\"}}"));
        registerConnector(CONNECTOR_NAME, getConnectorConfig(INPUT_TOPIC, WORKFLOW_NAME));

        await(() -> {
            SearchWfRunRequest criteria = SearchWfRunRequest.newBuilder()
                    .setStatus(LHStatus.COMPLETED)
                    .setWfSpecName(WORKFLOW_NAME)
                    .build();
            WfRunIdList result = lhClient.searchWfRun(criteria);
            assertThat(result.getResultsCount()).isOne();
        });
    }

    @Test
    public void shouldExecuteWfRunWithNativeMapContainingStructValuesAfterProducing() {
        registerStructDef(this, Pilot.class);
        startWorker(this);
        registerWorkflow(STRUCT_WORKFLOW);
        createTopics(STRUCT_INPUT_TOPIC);
        produceValues(
                STRUCT_INPUT_TOPIC,
                KafkaMessage.of(
                        "{\"lookup\":{\"1\":{\"name\":\"Luke\"},\"2\":{\"name\":\"Leia\"}}}"));
        registerConnector(
                STRUCT_CONNECTOR_NAME,
                getConnectorConfig(STRUCT_INPUT_TOPIC, STRUCT_WORKFLOW_NAME));

        await(() -> {
            SearchWfRunRequest criteria = SearchWfRunRequest.newBuilder()
                    .setStatus(LHStatus.COMPLETED)
                    .setWfSpecName(STRUCT_WORKFLOW_NAME)
                    .build();
            WfRunIdList result = lhClient.searchWfRun(criteria);
            assertThat(result.getResultsCount()).isOne();
        });
    }

    private static HashMap<String, Object> getConnectorConfig(
            String inputTopic, String workflowName) {
        HashMap<String, Object> connectorConfig = new HashMap<>();
        connectorConfig.put("tasks.max", 1);
        connectorConfig.put("connector.class", "io.littlehorse.connect.WfRunSinkConnector");
        connectorConfig.put("topics", inputTopic);
        connectorConfig.put("key.converter", "org.apache.kafka.connect.storage.StringConverter");
        connectorConfig.put("value.converter", "org.apache.kafka.connect.json.JsonConverter");
        connectorConfig.put("value.converter.schemas.enable", false);
        connectorConfig.put("lhc.api.port", 2023);
        connectorConfig.put("lhc.api.host", "littlehorse");
        connectorConfig.put("lhc.tenant.id", "default");
        connectorConfig.put("wf.spec.name", workflowName);
        return connectorConfig;
    }
}
