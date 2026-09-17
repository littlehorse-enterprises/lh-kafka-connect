package io.littlehorse.example;

import io.littlehorse.sdk.common.config.LHConfig;
import io.littlehorse.sdk.wfsdk.LHMapBuilder;
import io.littlehorse.sdk.wfsdk.WfRunVariable;
import io.littlehorse.sdk.wfsdk.Workflow;
import io.littlehorse.sdk.worker.LHTaskMethod;
import io.littlehorse.sdk.worker.LHTaskWorker;
import io.littlehorse.sdk.worker.LHType;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class Main {

    public static final String TASK_DEF_NAME = "example-wfrun-map-inspect";
    public static final String WF_NAME = "example-wfrun-map";
    public static final String VARIABLE_INVENTORY = "inventory";
    public static final String VARIABLE_REQUESTED_PRODUCT_ID = "requested-product-id";

    public static Workflow getWorkflow() {
        return Workflow.newWorkflow(WF_NAME, wf -> {
            WfRunVariable inventory =
                    wf.declareMap(VARIABLE_INVENTORY, Integer.class, String.class);
            WfRunVariable requestedProductId = wf.declareInt(VARIABLE_REQUESTED_PRODUCT_ID);

            LHMapBuilder selection = wf.buildMap()
                    .put(0, "not-requested")
                    .put(requestedProductId, inventory.get(requestedProductId));

            wf.execute(
                    TASK_DEF_NAME,
                    "before-put",
                    inventory,
                    requestedProductId,
                    inventory.get(requestedProductId),
                    selection);

            inventory.put(requestedProductId, "reserved");

            wf.execute(
                    TASK_DEF_NAME,
                    "after-put",
                    inventory,
                    requestedProductId,
                    inventory.get(requestedProductId),
                    selection);
        });
    }

    private static LHTaskWorker getTaskWorker(LHConfig lhConfig) {
        LHTaskWorker worker = new LHTaskWorker(new InventoryWorker(), TASK_DEF_NAME, lhConfig);
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

    public static class InventoryWorker {

        @LHTaskMethod(TASK_DEF_NAME)
        public String inspectInventory(
                String stage,
                @LHType(isLHMap = true) Map<Integer, String> inventory,
                long requestedProductId,
                String selectedProduct,
                @LHType(isLHMap = true) Map<Integer, String> selection) {
            String message = "%s: product %d is '%s'; inventory=%s; selection=%s"
                    .formatted(stage, requestedProductId, selectedProduct, inventory, selection);
            log.info(message);
            return message;
        }
    }
}
