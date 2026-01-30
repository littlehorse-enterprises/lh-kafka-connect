package io.littlehorse.example;

import io.littlehorse.sdk.common.config.LHConfig;
import io.littlehorse.sdk.common.proto.LittleHorseGrpc.LittleHorseBlockingStub;
import io.littlehorse.sdk.wfsdk.ExternalEventNodeOutput;
import io.littlehorse.sdk.wfsdk.Workflow;
import io.littlehorse.sdk.worker.LHTaskMethod;
import io.littlehorse.sdk.worker.LHTaskWorker;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class Main {

    public static final String TASK_DEF_NAME = "example-external-event-headers-squadron";
    public static final String WF_NAME = "example-external-event-headers";
    public static final String EXTERNAL_EVENT_NAME = "example-external-event-headers";

    public static Workflow getWorkflow() {
        return Workflow.newWorkflow(WF_NAME, wf -> {
            ExternalEventNodeOutput event =
                    wf.waitForEvent(EXTERNAL_EVENT_NAME).registeredAs(List.class);
            wf.execute(TASK_DEF_NAME, event);
        });
    }

    private static LHTaskWorker getTaskWorker(LHConfig lhConfig) {
        LHTaskWorker worker = new LHTaskWorker(new SquadronWorker(), TASK_DEF_NAME, lhConfig);
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

    public static class SquadronWorker {

        @LHTaskMethod(TASK_DEF_NAME)
        public String squadron(SquadronUnit[] squadron) {
            String message = "Squadron members: "
                    + Arrays.stream(squadron)
                            .map(SquadronUnit::toString)
                            .collect(Collectors.joining(", "));
            log.info(message);
            return message;
        }
    }
}
