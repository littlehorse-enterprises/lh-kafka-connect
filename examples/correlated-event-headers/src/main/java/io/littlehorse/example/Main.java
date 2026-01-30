package io.littlehorse.example;

import static io.littlehorse.sdk.common.proto.LittleHorseGrpc.LittleHorseBlockingStub;

import io.littlehorse.sdk.common.config.LHConfig;
import io.littlehorse.sdk.wfsdk.ExternalEventNodeOutput;
import io.littlehorse.sdk.wfsdk.WfRunVariable;
import io.littlehorse.sdk.wfsdk.Workflow;
import io.littlehorse.sdk.worker.LHTaskMethod;
import io.littlehorse.sdk.worker.LHTaskWorker;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class Main {

    public static final String TASK_DEF_NAME = "example-correlated-event-headers-character-name";
    public static final String WF_NAME = "example-correlated-event-headers";
    public static final String EXTERNAL_EVENT_NAME = "example-correlated-event-headers";
    public static final String ID_VARIABLE = "payment-id";

    public static Workflow getWorkflow() {
        return Workflow.newWorkflow(WF_NAME, wf -> {
            WfRunVariable variable = wf.declareStr(ID_VARIABLE);
            ExternalEventNodeOutput event = wf.waitForEvent(EXTERNAL_EVENT_NAME)
                    .withCorrelationId(variable)
                    .registeredAs(Map.class);
            wf.execute(TASK_DEF_NAME, event);
        });
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

        Workflow workflow = getWorkflow();
        workflow.registerWfSpec(stub);

        worker.start();
    }

    public static class PaymentWorker {

        @LHTaskMethod(TASK_DEF_NAME)
        public String processPayment(Payment payment) {
            String message = "You're buying a %s for %.2f credits. Pyment id: %s"
                    .formatted(payment.getDroid(), payment.getCredits(), payment.getId());
            log.info(message);
            return message;
        }
    }
}
