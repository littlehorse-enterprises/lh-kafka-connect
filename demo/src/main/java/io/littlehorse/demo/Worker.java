package io.littlehorse.demo;

import io.littlehorse.sdk.common.config.LHConfig;
import io.littlehorse.sdk.worker.LHTaskMethod;
import io.littlehorse.sdk.worker.LHTaskWorker;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Slf4j
@Command(name = "worker", description = "Runs worker..")
public class Worker implements Callable<Integer> {

    public static final String TASK_DEF_NAME = "greet";
    private final LHConfig lhConfig;

    public Worker(LHConfig lhConfig) {
        this.lhConfig = lhConfig;
    }

    @LHTaskMethod(TASK_DEF_NAME)
    public String greeting(String name) {
        String message = "Hello there! " + name;
        log.info(message);
        return message;
    }

    @Override
    public Integer call() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        LHTaskWorker worker = new LHTaskWorker(this, TASK_DEF_NAME, lhConfig);
        Runtime
            .getRuntime()
            .addShutdownHook(
                new Thread(() -> {
                    worker.close();
                    latch.countDown();
                })
            );
        worker.registerTaskDef();
        worker.start();
        latch.await();
        return CommandLine.ExitCode.OK;
    }
}
