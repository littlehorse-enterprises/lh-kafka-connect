package io.littlehorse.example;

import io.littlehorse.sdk.common.config.LHConfig;
import io.littlehorse.sdk.wfsdk.WfRunVariable;
import io.littlehorse.sdk.wfsdk.Workflow;
import io.littlehorse.sdk.worker.LHTaskMethod;
import io.littlehorse.sdk.worker.LHTaskWorker;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

    public static final String TASK_DEF_NAME =
            "example-wfrun-apicurio-json-schema-json-path-describe";
    public static final String WF_NAME = "example-wfrun-apicurio-json-schema-json-path";
    public static final String VARIABLE_NAME = "name";
    public static final String VARIABLE_CLIMATE = "climate";
    public static final String VARIABLE_TERRAIN = "terrain";
    public static final String VARIABLE_POPULATION = "population";

    public static Workflow getWorkflow() {
        return Workflow.newWorkflow(WF_NAME, wf -> {
            // The records carry the planet fields at the top level (no envelope), so the
            // JsonPathMapperTransform reshapes each record's value into these input variables.
            WfRunVariable name = wf.declareStr(VARIABLE_NAME);
            WfRunVariable climate = wf.declareStr(VARIABLE_CLIMATE);
            WfRunVariable terrain = wf.declareStr(VARIABLE_TERRAIN);
            WfRunVariable population = wf.declareInt(VARIABLE_POPULATION);
            wf.execute(TASK_DEF_NAME, name, climate, terrain, population);
        });
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
        public String describe(String name, String climate, String terrain, long population) {
            String message = String.format(
                    "%s is a %s world of %s, home to %d inhabitants.",
                    name, climate, terrain, population);
            log.info(message);
            return message;
        }
    }
}
