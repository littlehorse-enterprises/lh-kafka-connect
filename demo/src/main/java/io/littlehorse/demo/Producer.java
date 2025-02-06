package io.littlehorse.demo;

import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Callable;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Slf4j
@Command(name = "producer", description = "Produces messages to a topic.")
public class Producer implements Callable<Integer> {

    private final Faker faker = new Faker();
    private final Properties properties;

    @Parameters(index = "0", description = "Topic name.")
    private String topic;

    @Option(
        names = { "-n" },
        description = "Total new messages to produce. Default: ${DEFAULT-VALUE}.",
        defaultValue = "1"
    )
    private int messages;

    public Producer(Properties properties) {
        this.properties = properties;
    }

    @Override
    public Integer call() {
        properties.put(
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
            StringSerializer.class
        );
        properties.put(
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
            StringSerializer.class
        );
        KafkaProducer<String, String> producer = new KafkaProducer<>(
            properties
        );

        for (int i = 0; i < messages; i++) {
            String character = faker.starWars().character();

            ProducerRecord<String, String> record = new ProducerRecord<>(
                topic,
                UUID.randomUUID().toString(),
                character
            );

            producer.send(
                record,
                (metadata, exception) -> {
                    if (exception != null) {
                        log.error("Error producing {}", character, exception);
                        return;
                    }
                    log.info("Producing message: {}", character);
                }
            );
        }

        producer.flush();
        producer.close();

        return CommandLine.ExitCode.OK;
    }
}
