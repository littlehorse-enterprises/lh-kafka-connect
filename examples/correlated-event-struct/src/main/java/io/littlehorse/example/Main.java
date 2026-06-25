package io.littlehorse.example;

import static io.littlehorse.sdk.common.proto.LittleHorseGrpc.LittleHorseBlockingStub;

import io.littlehorse.sdk.common.config.LHConfig;
import io.littlehorse.sdk.common.proto.StructDefCompatibilityType;
import io.littlehorse.sdk.wfsdk.Workflow;
import io.littlehorse.sdk.worker.LHTaskMethod;
import io.littlehorse.sdk.worker.LHTaskWorker;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

    public static final String TASK_DEF_NAME = "example-correlated-event-struct-greet";
    public static final String WF_NAME = "example-correlated-event-struct";
    public static final String EXTERNAL_EVENT_NAME = "example-correlated-event-struct";
    public static final String ID_VARIABLE = "id";

    public static Workflow getWorkflow() {
        return Workflow.newWorkflow(
                WF_NAME,
                wf -> wf.execute(
                        TASK_DEF_NAME,
                        wf.waitForEvent(EXTERNAL_EVENT_NAME)
                                .withCorrelationId(wf.declareStr(ID_VARIABLE))
                                .registeredAs(Pilot.class)));
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

        // StructDefs must be registered before the TaskDef and WfSpec that use them.
        // Dependencies first: Vehicle is nested inside Pilot.
        worker.registerStructDef(Vehicle.class, StructDefCompatibilityType.NO_SCHEMA_UPDATES);
        worker.registerStructDef(Pilot.class, StructDefCompatibilityType.NO_SCHEMA_UPDATES);

        worker.registerTaskDef();

        Workflow workflow = getWorkflow();
        workflow.registerWfSpec(stub);

        worker.start();
    }

    public static class GreetingsWorker {

        @LHTaskMethod(TASK_DEF_NAME)
        public String greeting(Pilot pilot) {
            String message = "Hello there! " + pilot.getName() + ", you're driving a "
                    + pilot.getVehicle().getModel();
            log.info(message);
            return message;
        }
    }
}
