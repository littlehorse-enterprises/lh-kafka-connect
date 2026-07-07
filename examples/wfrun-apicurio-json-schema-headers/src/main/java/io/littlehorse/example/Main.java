package io.littlehorse.example;

import io.littlehorse.sdk.common.config.LHConfig;
import io.littlehorse.sdk.wfsdk.Workflow;
import io.littlehorse.sdk.worker.LHTaskMethod;
import io.littlehorse.sdk.worker.LHTaskWorker;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

    public static final String TASK_DEF_NAME = "example-wfrun-apicurio-json-schema-headers-greet";
    public static final String WF_NAME = "example-wfrun-apicurio-json-schema-headers";
    public static final String VARIABLE_WIELDER = "wielder";

    public static Workflow getWorkflow() {
        return Workflow.newWorkflow(
                WF_NAME, wf -> wf.execute(TASK_DEF_NAME, wf.declareJsonObj(VARIABLE_WIELDER)));
    }

    private static LHTaskWorker getTaskWorker(LHConfig lhConfig) {
        LHTaskWorker worker = new LHTaskWorker(new WielderWorker(), TASK_DEF_NAME, lhConfig);
        Runtime.getRuntime().addShutdownHook(new Thread(worker::close));
        return worker;
    }

    public static void main(String[] args) {
        LHConfig lhConfig = new LHConfig();

        LHTaskWorker worker = getTaskWorker(lhConfig);
        worker.registerTaskDef();

        Workflow workflow = getWorkflow();
        workflow.registerWfSpec(lhConfig.getBlockingStub());

        worker.start();
    }

    public static class WielderWorker {

        @LHTaskMethod(TASK_DEF_NAME)
        public String greet(ForceWielder wielder) {
            String side = "SITH".equals(wielder.getType()) ? "the dark side" : "the light side";
            String message = String.format(
                    "%s (%s) wields a %s lightsaber and serves %s.",
                    wielder.getName(), wielder.getType(), wielder.getLightsaberColor(), side);
            log.info(message);
            return message;
        }
    }
}
