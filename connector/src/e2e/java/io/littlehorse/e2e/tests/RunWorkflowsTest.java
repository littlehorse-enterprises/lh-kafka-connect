package io.littlehorse.e2e.tests;

import io.littlehorse.e2e.configs.E2ETest;
import io.littlehorse.sdk.common.proto.LHStatus;
import io.littlehorse.sdk.wfsdk.Workflow;
import io.littlehorse.sdk.worker.LHTaskMethod;
import io.littlehorse.test.LHWorkflow;
import io.littlehorse.test.WorkflowVerifier;
import org.junit.jupiter.api.Test;


public class RunWorkflowsTest extends E2ETest {

    private WorkflowVerifier verifier;

    @LHWorkflow("greetings")
    private Workflow workflow;

    @LHWorkflow("greetings")
    public Workflow wfGreetings() {
        return Workflow.newWorkflow("greetings", wf -> wf.execute("greetings", wf.declareStr("name")));
    }

    @LHTaskMethod("greetings")
    public String greetings(String name) {
        return String.format("Hello %s!", name);
    }

    @Test
    public void shouldExecuteWfRunAfterProducing() {
        verifier.prepareRun(workflow).waitForStatus(LHStatus.COMPLETED).start();
    }
}
