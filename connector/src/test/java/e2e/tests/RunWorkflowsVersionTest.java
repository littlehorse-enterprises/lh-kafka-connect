package e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import e2e.configs.E2ETest;

import io.littlehorse.sdk.common.LHLibUtil;
import io.littlehorse.sdk.common.proto.GetLatestWfSpecRequest;
import io.littlehorse.sdk.common.proto.LittleHorseGrpc.LittleHorseBlockingStub;
import io.littlehorse.sdk.common.proto.WfRun;
import io.littlehorse.sdk.common.proto.WfSpecId;
import io.littlehorse.sdk.wfsdk.Workflow;
import io.littlehorse.sdk.worker.LHTaskMethod;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

/**
 * Verifies the {@code WfRunSinkConnector} runs the {@code WfSpec} version selected by its
 * {@code wf.spec.major.version} / {@code wf.spec.revision} configuration.
 *
 * <p>Three versions of the same {@code WfSpec} are registered:
 *
 * <ul>
 *   <li>a base version (first major, revision 0),
 *   <li>a fully compatible body change that bumps the revision (first major, revision 1),
 *   <li>a breaking change that adds a required, searchable variable and bumps the major version
 *       (second major, revision 0).
 * </ul>
 *
 * The actual version numbers LittleHorse assigns are discovered at runtime (rather than
 * hardcoded), then three connectors are started:
 *
 * <ul>
 *   <li>without a version → must run the latest (second major, revision 0),
 *   <li>with only the first major version → must run its latest revision (first major, revision 1),
 *   <li>with both the first major and revision 0 → must run exactly that pinned version.
 * </ul>
 */
public class RunWorkflowsVersionTest extends E2ETest {

    public static final String WORKFLOW_NAME = "wf-run-version-test";
    public static final String TASK_NAME = "wf-run-version-test";

    private static final String LATEST_CONNECTOR = "wf-run-version-latest";
    private static final String MAJOR_CONNECTOR = "wf-run-version-major";
    private static final String PINNED_CONNECTOR = "wf-run-version-pinned";

    private static final String LATEST_TOPIC = "wf-run-version-latest";
    private static final String MAJOR_TOPIC = "wf-run-version-major";
    private static final String PINNED_TOPIC = "wf-run-version-pinned";

    /** Base version: a single required input variable. */
    private static final Workflow WORKFLOW_V1_0 =
            Workflow.newWorkflow(WORKFLOW_NAME, wf -> wf.execute(TASK_NAME, wf.declareStr("name")));

    /** Compatible body change (extra sleep) keeping the same signature: bumps the revision. */
    private static final Workflow WORKFLOW_V1_1 = Workflow.newWorkflow(WORKFLOW_NAME, wf -> {
        wf.execute(TASK_NAME, wf.declareStr("name"));
        wf.sleepSeconds(1);
    });

    /** Breaking change: adds a required, searchable variable: bumps the major version. */
    private static final Workflow WORKFLOW_V2_0 = Workflow.newWorkflow(WORKFLOW_NAME, wf -> {
        wf.declareInt("count").required().searchable();
        wf.execute(TASK_NAME, wf.declareStr("name"));
    });

    private final LittleHorseBlockingStub lhClient = getLittleHorseConfig().getBlockingStub();

    @LHTaskMethod(TASK_NAME)
    public String greetings(String name) {
        String message = String.format("Hello %s!", name);
        log.info("Executing worker, output: {}", message);
        return message;
    }

    @Test
    public void shouldRunTheWfSpecVersionSelectedByTheConnector() {
        startWorker(this);

        // Register the three versions in order, capturing the version LittleHorse assigns to each
        // straight from the PutWfSpec response (authoritative, no read-after-write race).
        WfSpecId firstVersion = putWorkflow(WORKFLOW_V1_0);
        WfSpecId firstMajorLatest = putWorkflow(WORKFLOW_V1_1);
        WfSpecId latestVersion = putWorkflow(WORKFLOW_V2_0);

        // The compatible change must bump the revision; the breaking change the major version.
        assertThat(firstMajorLatest.getMajorVersion()).isEqualTo(firstVersion.getMajorVersion());
        assertThat(firstMajorLatest.getRevision()).isEqualTo(firstVersion.getRevision() + 1);
        assertThat(latestVersion.getMajorVersion()).isEqualTo(firstVersion.getMajorVersion() + 1);
        assertThat(latestVersion.getRevision()).isZero();

        // Ensure the connectors will observe a consistent view when they resolve versions.
        awaitLatestEquals(latestVersion);
        awaitLatestForMajorEquals(firstVersion.getMajorVersion(), firstMajorLatest);

        createTopics(LATEST_TOPIC, MAJOR_TOPIC, PINNED_TOPIC);

        // The latest version requires both "count" and "name"; the earlier ones only "name".
        produceValues(LATEST_TOPIC, KafkaMessage.of("{\"name\":\"Luke\",\"count\":5}"));
        produceValues(MAJOR_TOPIC, KafkaMessage.of("{\"name\":\"Leia\"}"));
        produceValues(PINNED_TOPIC, KafkaMessage.of("{\"name\":\"Han\"}"));

        // No version: must run the latest (second major, revision 0).
        registerConnector(
                LATEST_CONNECTOR, getConnectorConfig(LATEST_CONNECTOR, LATEST_TOPIC, null, null));
        // Only the first major: must run its latest revision (first major, revision 1).
        registerConnector(
                MAJOR_CONNECTOR,
                getConnectorConfig(
                        MAJOR_CONNECTOR, MAJOR_TOPIC, firstVersion.getMajorVersion(), null));
        // First major and revision 0: must run exactly that pinned version.
        registerConnector(
                PINNED_CONNECTOR,
                getConnectorConfig(
                        PINNED_CONNECTOR,
                        PINNED_TOPIC,
                        firstVersion.getMajorVersion(),
                        firstVersion.getRevision()));

        assertWfRunUsesVersion(LATEST_CONNECTOR, LATEST_TOPIC, latestVersion);
        assertWfRunUsesVersion(MAJOR_CONNECTOR, MAJOR_TOPIC, firstMajorLatest);
        assertWfRunUsesVersion(PINNED_CONNECTOR, PINNED_TOPIC, firstVersion);
    }

    private void assertWfRunUsesVersion(String connectorName, String topic, WfSpecId expected) {
        String wfRunId = "%s-%s-0-0".formatted(connectorName, topic);
        await(() -> {
            WfRun wfRun = lhClient.getWfRun(LHLibUtil.wfRunIdFromString(wfRunId));
            assertThat(wfRun.getWfSpecId().getMajorVersion()).isEqualTo(expected.getMajorVersion());
            assertThat(wfRun.getWfSpecId().getRevision()).isEqualTo(expected.getRevision());
        });
    }

    private WfSpecId putWorkflow(Workflow workflow) {
        return lhClient.putWfSpec(workflow.compileWorkflow(getLittleHorseConfig()))
                .getId();
    }

    private void awaitLatestEquals(WfSpecId expected) {
        await(() -> {
            WfSpecId actual = latestWfSpecId();
            assertThat(actual.getMajorVersion()).isEqualTo(expected.getMajorVersion());
            assertThat(actual.getRevision()).isEqualTo(expected.getRevision());
        });
    }

    private void awaitLatestForMajorEquals(int majorVersion, WfSpecId expected) {
        await(() -> {
            WfSpecId actual = latestWfSpecId(majorVersion);
            assertThat(actual.getMajorVersion()).isEqualTo(expected.getMajorVersion());
            assertThat(actual.getRevision()).isEqualTo(expected.getRevision());
        });
    }

    private WfSpecId latestWfSpecId() {
        return lhClient.getLatestWfSpec(GetLatestWfSpecRequest.newBuilder()
                        .setName(WORKFLOW_NAME)
                        .build())
                .getId();
    }

    private WfSpecId latestWfSpecId(int majorVersion) {
        return lhClient.getLatestWfSpec(GetLatestWfSpecRequest.newBuilder()
                        .setName(WORKFLOW_NAME)
                        .setMajorVersion(majorVersion)
                        .build())
                .getId();
    }

    private static HashMap<String, Object> getConnectorConfig(
            String connectorName, String topic, Integer majorVersion, Integer revision) {
        HashMap<String, Object> connectorConfig = new HashMap<>();
        connectorConfig.put("name", connectorName);
        connectorConfig.put("tasks.max", 1);
        connectorConfig.put("connector.class", "io.littlehorse.connect.WfRunSinkConnector");
        connectorConfig.put("topics", topic);
        connectorConfig.put("key.converter", "org.apache.kafka.connect.storage.StringConverter");
        connectorConfig.put("value.converter", "org.apache.kafka.connect.json.JsonConverter");
        connectorConfig.put("value.converter.schemas.enable", false);
        connectorConfig.put("lhc.api.port", 2023);
        connectorConfig.put("lhc.api.host", "littlehorse");
        connectorConfig.put("lhc.tenant.id", "default");
        connectorConfig.put("wf.spec.name", WORKFLOW_NAME);
        if (majorVersion != null) {
            connectorConfig.put("wf.spec.major.version", majorVersion);
        }
        if (revision != null) {
            connectorConfig.put("wf.spec.revision", revision);
        }
        return connectorConfig;
    }
}
