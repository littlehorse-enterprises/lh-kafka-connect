package io.littlehorse.demo;

import io.littlehorse.sdk.common.config.LHConfig;
import io.littlehorse.sdk.common.proto.VariableType;
import io.littlehorse.sdk.wfsdk.WfRunVariable;
import io.littlehorse.sdk.wfsdk.Workflow;
import io.littlehorse.sdk.wfsdk.internal.WorkflowImpl;
import java.util.concurrent.Callable;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Slf4j
@Command(name = "register", description = "Registers the workflow.")
public class Register implements Callable<Integer> {

    private final LHConfig lhConfig;

    public Register(LHConfig lhConfig) {
        this.lhConfig = lhConfig;
    }

    public static Workflow getWorkflow() {
        return new WorkflowImpl(
            "demo",
            wf -> {
                WfRunVariable theName = wf.addVariable(
                    "name",
                    VariableType.STR
                );
                wf.execute("greet", theName);
            }
        );
    }

    @Override
    public Integer call() {
        Workflow workflow = getWorkflow();
        workflow.registerWfSpec(lhConfig.getBlockingStub());
        return CommandLine.ExitCode.OK;
    }
}
