package io.littlehorse.demo;

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
        Properties props = new Properties();
        props.load(
            Main.class.getClassLoader()
                .getResourceAsStream("producer.properties")
        );

        CommandLine commandLine = new CommandLine(new Main())
            .addSubcommand(new Producer(props));

        System.exit(commandLine.execute(args));
    }

    @Override
    public void run() {
        throw new CommandLine.ParameterException(
            spec.commandLine(),
            "Missing required subcommand"
        );
    }
}
