package e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import e2e.configs.E2ETest;

import io.littlehorse.sdk.common.proto.GetLatestWfSpecRequest;
import io.littlehorse.sdk.common.proto.LHStatus;
import io.littlehorse.sdk.common.proto.LittleHorseGrpc.LittleHorseBlockingStub;
import io.littlehorse.sdk.common.proto.SearchWfRunRequest;
import io.littlehorse.sdk.common.proto.StructDef;
import io.littlehorse.sdk.common.proto.StructDefCompatibilityType;
import io.littlehorse.sdk.common.proto.StructDefId;
import io.littlehorse.sdk.common.proto.ThreadSpec;
import io.littlehorse.sdk.common.proto.ThreadVarDef;
import io.littlehorse.sdk.common.proto.Variable;
import io.littlehorse.sdk.common.proto.VariableId;
import io.littlehorse.sdk.common.proto.VariableValue;
import io.littlehorse.sdk.common.proto.WfRunId;
import io.littlehorse.sdk.common.proto.WfRunIdList;
import io.littlehorse.sdk.common.proto.WfSpec;
import io.littlehorse.sdk.wfsdk.Workflow;
import io.littlehorse.sdk.worker.LHStructDef;
import io.littlehorse.sdk.worker.LHStructField;
import io.littlehorse.sdk.worker.LHTaskMethod;
import io.littlehorse.sdk.worker.LHTaskWorker;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

/**
 * Verifies the {@code WfRunSinkConnector} builds {@code STRUCT} variables using the exact
 * {@code StructDef} version pinned by the {@code WfSpec}, not the latest one. A first struct version
 * ({@code name} only) is registered and the {@code WfSpec} is registered against it, pinning that
 * version. A second, fully compatible version that adds a nullable {@code callsign} field is then
 * registered. A record carrying both fields is produced; because the connector resolves the
 * versioned {@link StructDefId} from the {@code WfSpec}, it builds the struct from the pinned
 * version, dropping {@code callsign}. The test reads the variable back and asserts its struct
 * carries the pinned version and omits the field that only exists in the newer version.
 */
public class RunWorkflowsStructVersionTest extends E2ETest {

    public static final String WORKFLOW_NAME = "struct-version-workflow";
    public static final String CONNECTOR_NAME = "struct-version-workflow";
    public static final String TASK_NAME = "struct-version-workflow";
    private static final String STRUCT_NAME = "struct-version-pilot";
    private static final String INPUT_TOPIC = "struct-version";
    private static final String VARIABLE = "pilot";
    private static final Workflow WORKFLOW = Workflow.newWorkflow(WORKFLOW_NAME, wf -> {
        wf.declareStruct(VARIABLE, PilotV0.class);
        wf.sleepSeconds(1);
    });
    private final LittleHorseBlockingStub lhClient = getLittleHorseConfig().getBlockingStub();

    /** First struct version: only {@code name}. */
    @LHStructDef(STRUCT_NAME)
    public static class PilotV0 {

        private String name;

        public PilotV0() {}

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    /** Second, fully compatible struct version: adds a nullable {@code callsign}. */
    @LHStructDef(STRUCT_NAME)
    public static class PilotV1 {

        private String name;

        @LHStructField(isNullable = true)
        private String callsign;

        public PilotV1() {}

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCallsign() {
            return callsign;
        }

        public void setCallsign(String callsign) {
            this.callsign = callsign;
        }
    }

    // Required so a LHTaskWorker can be constructed to register the struct definitions; the task is
    // never started because the workflow only declares the variable and sleeps.
    @LHTaskMethod(TASK_NAME)
    public String unused(String in) {
        return in;
    }

    @Test
    public void shouldBuildStructUsingTheVersionPinnedByTheWfSpec() {
        // Register v0, then the WfSpec (which pins v0), then the newer v1.
        registerStructVersion(PilotV0.class);
        registerWorkflow(WORKFLOW);
        registerStructVersion(PilotV1.class);

        int pinnedVersion = pinnedStructVersion();

        // Sanity check: a newer version exists and it does carry the extra field. Struct
        // registration is eventually consistent, so poll until the newer version is queryable.
        await(() -> {
            StructDef newerVersion = lhClient.getStructDef(StructDefId.newBuilder()
                    .setName(STRUCT_NAME)
                    .setVersion(pinnedVersion + 1)
                    .build());
            assertThat(newerVersion.getStructDef().getFieldsMap()).containsKey("callsign");
        });

        createTopics(INPUT_TOPIC);
        // The record carries both fields; "callsign" only exists in the newer version.
        produceValues(
                INPUT_TOPIC,
                KafkaMessage.of("{\"pilot\":{\"name\":\"Luke\",\"callsign\":\"Red Five\"}}"));
        registerConnector(CONNECTOR_NAME, getConnectorConfig());

        WfRunId wfRunId = WfRunId.newBuilder()
                .setId("%s-%s-0-0".formatted(CONNECTOR_NAME, INPUT_TOPIC))
                .build();

        await(() -> {
            SearchWfRunRequest criteria = SearchWfRunRequest.newBuilder()
                    .setStatus(LHStatus.COMPLETED)
                    .setWfSpecName(WORKFLOW_NAME)
                    .build();
            WfRunIdList result = lhClient.searchWfRun(criteria);
            assertThat(result.getResultsList()).contains(wfRunId);
        });

        Variable pilot = lhClient.getVariable(VariableId.newBuilder()
                .setWfRunId(wfRunId)
                .setThreadRunNumber(0)
                .setName(VARIABLE)
                .build());
        VariableValue value = pilot.getValue();

        // The connector built the struct from the version the WfSpec pinned, not the latest.
        assertThat(value.hasStruct()).isTrue();
        assertThat(value.getStruct().getStructDefId().getVersion()).isEqualTo(pinnedVersion);
        // Only the pinned version's fields are present; the newer "callsign" was dropped.
        assertThat(value.getStruct().getStruct().containsFields("name")).isTrue();
        assertThat(value.getStruct().getStruct().containsFields("callsign")).isFalse();
    }

    private int pinnedStructVersion() {
        WfSpec wfSpec = lhClient.getLatestWfSpec(
                GetLatestWfSpecRequest.newBuilder().setName(WORKFLOW_NAME).build());
        ThreadSpec entrypoint = wfSpec.getThreadSpecsOrThrow(wfSpec.getEntrypointThreadName());
        return entrypoint.getVariableDefsList().stream()
                .map(ThreadVarDef::getVarDef)
                .filter(varDef -> varDef.getName().equals(VARIABLE))
                .findFirst()
                .orElseThrow()
                .getTypeDef()
                .getStructDefId()
                .getVersion();
    }

    private void registerStructVersion(Class<?> structClass) {
        // The worker shares the cached LHConfig channel, so it must not be closed here.
        @SuppressWarnings("resource")
        LHTaskWorker worker = new LHTaskWorker(this, TASK_NAME, getLittleHorseConfig());
        await(() -> worker.registerStructDef(
                structClass, StructDefCompatibilityType.FULLY_COMPATIBLE_SCHEMA_UPDATES));
    }

    private static HashMap<String, Object> getConnectorConfig() {
        HashMap<String, Object> connectorConfig = new HashMap<>();
        connectorConfig.put("tasks.max", 1);
        connectorConfig.put("connector.class", "io.littlehorse.connect.WfRunSinkConnector");
        connectorConfig.put("topics", INPUT_TOPIC);
        connectorConfig.put("key.converter", "org.apache.kafka.connect.storage.StringConverter");
        connectorConfig.put("value.converter", "org.apache.kafka.connect.json.JsonConverter");
        connectorConfig.put("value.converter.schemas.enable", false);
        connectorConfig.put("lhc.api.port", 2023);
        connectorConfig.put("lhc.api.host", "littlehorse");
        connectorConfig.put("lhc.tenant.id", "default");
        connectorConfig.put("wf.spec.name", WORKFLOW_NAME);
        return connectorConfig;
    }
}
