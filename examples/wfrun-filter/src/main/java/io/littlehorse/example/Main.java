package io.littlehorse.example;

import io.littlehorse.sdk.common.config.LHConfig;
import io.littlehorse.sdk.common.proto.VariableType;
import io.littlehorse.sdk.wfsdk.Workflow;
import io.littlehorse.sdk.wfsdk.WorkflowThread;
import io.littlehorse.sdk.wfsdk.internal.WorkflowImpl;
import io.littlehorse.sdk.worker.LHTaskMethod;
import io.littlehorse.sdk.worker.LHTaskWorker;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

    public static final String TASK_DEF_NAME = "example-wfrun-filter-quotes";
    public static final String WF_NAME = "example-wfrun-filter";
    public static final String VARIABLE_QUOTE = "quote";

    public static Workflow getWorkflow() {
        return new WorkflowImpl(WF_NAME, Main::buildWf);
    }

    private static void buildWf(WorkflowThread wf) {
        wf.execute(TASK_DEF_NAME, wf.addVariable(VARIABLE_QUOTE, VariableType.STR));
    }

    private static LHTaskWorker getTaskWorker(LHConfig lhConfig) {
        LHTaskWorker worker = new LHTaskWorker(new QuotesWorker(), TASK_DEF_NAME, lhConfig);
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

    public static class QuotesWorker {

        @LHTaskMethod(TASK_DEF_NAME)
        public String quotesPrinter(String quote) {
            log.info(quote);
            return quote;
        }
    }
}
