package io.littlehorse.example;

import io.littlehorse.sdk.common.config.LHConfig;
import io.littlehorse.sdk.common.proto.PutExternalEventDefRequest;
import io.littlehorse.sdk.wfsdk.WfRunVariable;
import io.littlehorse.sdk.wfsdk.Workflow;
import io.littlehorse.sdk.worker.LHTaskMethod;
import io.littlehorse.sdk.worker.LHTaskWorker;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class Main {

    public static final String TASK_DEF_NAME = "example-external-event-json-squadron";
    public static final String WF_NAME = "example-external-event-json";
    public static final String VARIABLE_SQUADRON = "squadron";
    public static final String EXTERNAL_EVENT_NAME = "set-squadron-members";

    public static Workflow getWorkflow() {
        return Workflow.newWorkflow(WF_NAME, wf -> {
            WfRunVariable squadronMembers = wf.declareJsonArr(VARIABLE_SQUADRON);
            squadronMembers.assign(wf.waitForEvent(EXTERNAL_EVENT_NAME));
            wf.execute(TASK_DEF_NAME, squadronMembers);
        });
    }

    private static LHTaskWorker getTaskWorker(LHConfig lhConfig) {
        LHTaskWorker worker = new LHTaskWorker(new SquadronWorker(), TASK_DEF_NAME, lhConfig);
        Runtime.getRuntime().addShutdownHook(new Thread(worker::close));
        return worker;
    }

    public static void main(String[] args) {
        LHConfig lhConfig = new LHConfig();

        LHTaskWorker worker = getTaskWorker(lhConfig);
        worker.registerTaskDef();

        Workflow workflow = getWorkflow();
        Set<String> externalEventNames = workflow.getRequiredExternalEventDefNames();
        for (String externalEventName : externalEventNames) {
            lhConfig.getBlockingStub()
                    .putExternalEventDef(PutExternalEventDefRequest.newBuilder()
                            .setName(externalEventName)
                            .build());
        }
        workflow.registerWfSpec(lhConfig.getBlockingStub());

        worker.start();
    }

    public static class SquadronWorker {

        @LHTaskMethod(TASK_DEF_NAME)
        public String squadron(List<Map<String, Object>> squadron) {
            String message = "Squadron members: "
                    + squadron.stream()
                            .flatMap(object -> object.values().stream())
                            .map(Object::toString)
                            .collect(Collectors.joining(", "));
            log.info(message);
            return message;
        }
    }
}
