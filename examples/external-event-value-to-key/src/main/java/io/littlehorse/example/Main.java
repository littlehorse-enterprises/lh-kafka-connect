package io.littlehorse.example;

import static io.littlehorse.sdk.common.proto.LittleHorseGrpc.LittleHorseBlockingStub;

import io.littlehorse.sdk.common.config.LHConfig;
import io.littlehorse.sdk.wfsdk.Workflow;
import io.littlehorse.sdk.worker.LHTaskMethod;
import io.littlehorse.sdk.worker.LHTaskWorker;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

    public static final String TASK_DEF_NAME = "example-external-event-value-to-key-character-name";
    public static final String WF_NAME = "example-external-event-value-to-key";
    public static final String EXTERNAL_EVENT_NAME = "example-external-event-value-to-key";

    public static Workflow getWorkflow() {
        return Workflow.newWorkflow(
                WF_NAME,
                wf -> wf.execute(
                        TASK_DEF_NAME,
                        wf.waitForEvent(EXTERNAL_EVENT_NAME).registeredAs(String.class)));
    }

    private static LHTaskWorker getTaskWorker(LHConfig lhConfig) {
        LHTaskWorker worker = new LHTaskWorker(new GreetingsWorker(), TASK_DEF_NAME, lhConfig);
        Runtime.getRuntime().addShutdownHook(new Thread(worker::close));
        return worker;
    }

    public static void main(String[] args) {
        LHConfig lhConfig = new LHConfig();
        LittleHorseBlockingStub stub = lhConfig.getBlockingStub();

        LHTaskWorker worker = getTaskWorker(lhConfig);
        worker.registerTaskDef();

        Workflow workflow = getWorkflow();
        workflow.registerWfSpec(stub);

        worker.start();
    }

    public static class GreetingsWorker {

        @LHTaskMethod(TASK_DEF_NAME)
        public String character(String characterName) {
            String message = "Hello " + characterName;
            log.info(message);
            return message;
        }
    }
}
