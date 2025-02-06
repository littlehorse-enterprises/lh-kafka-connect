package io.littlehorse.demo;

import io.littlehorse.sdk.common.config.LHConfig;
import java.io.IOException;
import java.util.Properties;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(name = "demo")
public class Main implements Runnable {

    @Spec
    private CommandSpec spec;

    public static void main(String[] args) throws IOException {
        LHConfig lhConfig = new LHConfig(
            getProperties("littlehorse.properties")
        );

        // commands
        Producer producer = new Producer(getProperties("producer.properties"));
        Worker worker = new Worker(lhConfig);
        Register register = new Register(lhConfig);

        CommandLine commandLine = new CommandLine(new Main())
            .addSubcommand(producer)
            .addSubcommand(register)
            .addSubcommand(worker);

        System.exit(commandLine.execute(args));
    }

    private static Properties getProperties(String name) throws IOException {
        Properties props = new Properties();
        props.load(Main.class.getClassLoader().getResourceAsStream(name));
        return props;
    }

    @Override
    public void run() {
        throw new CommandLine.ParameterException(
            spec.commandLine(),
            "Missing required subcommand"
        );
    }
}
