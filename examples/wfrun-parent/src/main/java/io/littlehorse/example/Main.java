package io.littlehorse.example;

import io.littlehorse.sdk.common.config.LHConfig;
import io.littlehorse.sdk.wfsdk.Workflow;
import io.littlehorse.sdk.worker.LHTaskMethod;
import io.littlehorse.sdk.worker.LHTaskWorker;
import io.littlehorse.sdk.worker.WorkerContext;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

    public static final String TASK_DEF_NAME = "example-wfrun-parent";
    public static final String PARENT_WF_NAME = "example-wfrun-parent";
    public static final String CHILD_WF_NAME = "example-wfrun-child";
    public static final String VARIABLE_NAME = "name";

    public static Workflow getParentWf() {
        return Workflow.newWorkflow(
                PARENT_WF_NAME,
                wf -> wf.execute(
                        TASK_DEF_NAME,
                        PARENT_WF_NAME,
                        wf.declareStr(VARIABLE_NAME).asPublic()));
    }

    public static Workflow getChildWf() {
        Workflow workflow = Workflow.newWorkflow(
                CHILD_WF_NAME,
                wf -> wf.execute(
                        TASK_DEF_NAME,
                        CHILD_WF_NAME,
                        wf.declareStr(VARIABLE_NAME).asInherited()));
        workflow.setParent(PARENT_WF_NAME);
        return workflow;
    }

    private static LHTaskWorker getTaskWorker(LHConfig lhConfig) {
        LHTaskWorker worker = new LHTaskWorker(new GreetingsWorker(), TASK_DEF_NAME, lhConfig);
        Runtime.getRuntime().addShutdownHook(new Thread(worker::close));
        return worker;
    }

    public static void main(String[] args) {
        LHConfig lhConfig = new LHConfig();

        LHTaskWorker worker = getTaskWorker(lhConfig);
        worker.registerTaskDef();

        Workflow parentWf = getParentWf();
        parentWf.registerWfSpec(lhConfig.getBlockingStub());

        Workflow childWf = getChildWf();
        childWf.registerWfSpec(lhConfig.getBlockingStub());

        worker.start();
    }

    public static class GreetingsWorker {

        @LHTaskMethod(TASK_DEF_NAME)
        public String greeting(String wfName, String name, WorkerContext context) {
            String message = "Hello %s! from %s, and my wfRunId is: %s"
                    .formatted(name, wfName, context.getWfRunId().getId());
            log.info(message);
            return message;
        }
    }
}
