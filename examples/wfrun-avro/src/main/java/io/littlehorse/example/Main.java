package io.littlehorse.example;

import io.littlehorse.sdk.common.config.LHConfig;
import io.littlehorse.sdk.wfsdk.Workflow;
import io.littlehorse.sdk.worker.LHTaskMethod;
import io.littlehorse.sdk.worker.LHTaskWorker;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

    public static final String TASK_DEF_NAME = "example-wfrun-avro-planet";
    public static final String WF_NAME = "example-wfrun-avro";
    public static final String VARIABLE_PLANET = "planet";

    public static Workflow getWorkflow() {
        return Workflow.newWorkflow(
                WF_NAME, wf -> wf.execute(TASK_DEF_NAME, wf.declareJsonObj(VARIABLE_PLANET)));
    }

    private static LHTaskWorker getTaskWorker(LHConfig lhConfig) {
        LHTaskWorker worker = new LHTaskWorker(new PlanetWorker(), TASK_DEF_NAME, lhConfig);
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

    public static class PlanetWorker {

        @LHTaskMethod(TASK_DEF_NAME)
        public String describePlanet(Planet planet) {
            String message = "This is " + planet.getName() + ", " + planet.getPopulation()
                    + " people live here";
            log.info(message);
            return message;
        }
    }
}
