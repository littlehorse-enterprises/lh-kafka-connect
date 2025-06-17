package io.littlehorse.example;

import static io.littlehorse.sdk.common.proto.LittleHorseGrpc.LittleHorseBlockingStub;

import io.littlehorse.sdk.common.config.LHConfig;
import io.littlehorse.sdk.common.proto.CorrelatedEventConfig;
import io.littlehorse.sdk.common.proto.PutExternalEventDefRequest;
import io.littlehorse.sdk.common.proto.ReturnType;
import io.littlehorse.sdk.common.proto.TypeDefinition;
import io.littlehorse.sdk.common.proto.VariableType;
import io.littlehorse.sdk.wfsdk.Workflow;
import io.littlehorse.sdk.worker.LHTaskMethod;
import io.littlehorse.sdk.worker.LHTaskWorker;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

    public static final String TASK_DEF_NAME = "example-correlated-event-json-character-name";
    public static final String WF_NAME = "example-correlated-event-json";
    public static final String EXTERNAL_EVENT_NAME = "example-correlated-event-json";
    public static final String ID_VARIABLE = "payment-id";

    public static Workflow getWorkflow() {
        return Workflow.newWorkflow(
                WF_NAME,
                wf -> wf.execute(
                        TASK_DEF_NAME,
                        wf.waitForEvent(EXTERNAL_EVENT_NAME)
                                .withCorrelationId(wf.declareStr(ID_VARIABLE))));
    }

    private static LHTaskWorker getTaskWorker(LHConfig lhConfig) {
        LHTaskWorker worker = new LHTaskWorker(new PaymentWorker(), TASK_DEF_NAME, lhConfig);
        Runtime.getRuntime().addShutdownHook(new Thread(worker::close));
        return worker;
    }

    public static void main(String[] args) {
        LHConfig lhConfig = new LHConfig();
        LittleHorseBlockingStub stub = lhConfig.getBlockingStub();

        LHTaskWorker worker = getTaskWorker(lhConfig);
        worker.registerTaskDef();

        // TODO: remove this
        stub.putExternalEventDef(PutExternalEventDefRequest.newBuilder()
                .setName(EXTERNAL_EVENT_NAME)
                .setContentType(ReturnType.newBuilder()
                        .setReturnType(TypeDefinition.newBuilder().setType(VariableType.JSON_OBJ)))
                .setCorrelatedEventConfig(
                        CorrelatedEventConfig.newBuilder().setDeleteAfterFirstCorrelation(false))
                .build());

        Workflow workflow = getWorkflow();
        workflow.registerWfSpec(stub);

        worker.start();
    }

    public static class PaymentWorker {

        @LHTaskMethod(TASK_DEF_NAME)
        public String processPayment(Droid droid) {
            String message = "You're buying a %s for %.2f credits"
                    .formatted(droid.getName(), droid.getCredits());
            log.info(message);
            return message;
        }
    }
}
