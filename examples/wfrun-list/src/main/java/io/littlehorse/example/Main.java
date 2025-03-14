package io.littlehorse.example;

import io.littlehorse.sdk.common.config.LHConfig;
import io.littlehorse.sdk.wfsdk.Workflow;
import io.littlehorse.sdk.worker.LHTaskMethod;
import io.littlehorse.sdk.worker.LHTaskWorker;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class Main {

    public static final String TASK_DEF_NAME = "example-wfrun-list-droids";
    public static final String WF_NAME = "example-wfrun-list";
    public static final String VARIABLE_DROIDS = "droids";

    public static Workflow getWorkflow() {
        return Workflow.newWorkflow(
                WF_NAME, wf -> wf.execute(TASK_DEF_NAME, wf.declareJsonArr(VARIABLE_DROIDS)));
    }

    private static LHTaskWorker getTaskWorker(LHConfig lhConfig) {
        LHTaskWorker worker = new LHTaskWorker(new DroidsWorker(), TASK_DEF_NAME, lhConfig);
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

    public static class DroidsWorker {

        @LHTaskMethod(TASK_DEF_NAME)
        public String listDroids(List<Map<String, Object>> droids) {
            String message = "List of droids: "
                    + droids.stream()
                            .flatMap(object -> object.values().stream())
                            .map(Object::toString)
                            .collect(Collectors.joining(", "));
            log.info(message);
            return message;
        }
    }
}
