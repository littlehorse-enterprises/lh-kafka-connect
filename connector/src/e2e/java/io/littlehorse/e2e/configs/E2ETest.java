package io.littlehorse.e2e.configs;

import io.littlehorse.container.LittleHorseContainer;
import io.littlehorse.sdk.common.config.LHConfig;
import io.littlehorse.sdk.common.proto.PutExternalEventDefRequest;
import io.littlehorse.sdk.wfsdk.Workflow;
import io.littlehorse.sdk.worker.LHTaskMethod;
import io.littlehorse.sdk.worker.LHTaskWorker;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.serialization.StringSerializer;
import org.awaitility.Awaitility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public abstract class E2ETest {
    public static final String LH_INTERNAL_BOOTSTRAP_SERVER = "littlehorse";
    private static final String KAFKA_INTERNAL_BOOTSTRAP_SERVER = "kafka:19092";
    private static final Network NETWORK = Network.newNetwork();
    private static final String CONFLUENT_VERSION =
            System.getProperty("confluentVersion", "latest");
    private static final String LH_VERSION = System.getProperty("lhVersion", "latest");
    private static final DockerImageName KAFKA_CONNECT_IMAGE =
            DockerImageName.parse("confluentinc/cp-kafka-connect").withTag(CONFLUENT_VERSION);
    private static final DockerImageName KAFKA_IMAGE =
            DockerImageName.parse("confluentinc/cp-kafka").withTag(CONFLUENT_VERSION);
    private static final ConfluentKafkaContainer KAFKA = new ConfluentKafkaContainer(KAFKA_IMAGE)
            .withListener(KAFKA_INTERNAL_BOOTSTRAP_SERVER)
            .withNetwork(NETWORK);
    private static final KafkaConnectContainer KAFKA_CONNECT = new KafkaConnectContainer(
                    KAFKA_CONNECT_IMAGE)
            .dependsOn(KAFKA)
            .withKafkaBootstrapServers(KAFKA_INTERNAL_BOOTSTRAP_SERVER)
            .withNetwork(NETWORK)
            .withPluginsFromHost("./build/bundle");
    private static final DockerImageName LH_IMAGE = DockerImageName.parse(
                    "ghcr.io/littlehorse-enterprises/littlehorse/lh-server")
            .withTag(LH_VERSION);
    private static final LittleHorseContainer LITTLEHORSE = new LittleHorseContainer(LH_IMAGE)
            .dependsOn(KAFKA)
            .withInternalAdvertisedHost(LH_INTERNAL_BOOTSTRAP_SERVER)
            .withKafkaBootstrapServers(KAFKA_INTERNAL_BOOTSTRAP_SERVER)
            .withNetwork(NETWORK);
    protected static Logger log = LoggerFactory.getLogger(E2ETest.class);

    static {
        KAFKA.start();
        KAFKA_CONNECT.start();
        LITTLEHORSE.start();
    }

    private LHConfig lhConfig;

    public String getKafkaConnectUrl(String... paths) {
        String path = Arrays.stream(paths)
                .map(part -> "/" + part)
                .map(part -> part.replaceAll("/+", "/"))
                .collect(Collectors.joining());
        return KAFKA_CONNECT.getUrl() + path;
    }

    public String getKafkaBootstrapServers() {
        return KAFKA.getBootstrapServers();
    }

    public LHConfig getLittleHorseConfig() {
        if (lhConfig == null) {
            lhConfig = new LHConfig(LITTLEHORSE.getClientProperties());
        }
        return lhConfig;
    }

    public void registerWorkflow(Workflow workflow) {
        await(() -> {
            workflow.getRequiredExternalEventDefNames().forEach(name -> {
                PutExternalEventDefRequest request =
                        PutExternalEventDefRequest.newBuilder().setName(name).build();
                getLittleHorseConfig().getBlockingStub().putExternalEventDef(request);
            });
            workflow.registerWfSpec(getLittleHorseConfig().getBlockingStub());
        });
    }

    public void registerConnector(String connectorName, Map<String, Object> config) {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(config)
                .when()
                .put(getKafkaConnectUrl("connectors", connectorName, "config"))
                .then()
                .assertThat()
                .statusCode(201)
                .log()
                .everything();
    }

    public void await(Runnable runnable) {
        Awaitility.with()
                .pollInterval(Duration.ofSeconds(1))
                .ignoreExceptions()
                .await()
                .atMost(Duration.ofSeconds(30))
                .until(() -> {
                    try {
                        runnable.run();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    return true;
                });
    }

    public void startWorker(Object executable) {
        Arrays.stream(executable.getClass().getMethods())
                .filter(method -> method.isAnnotationPresent(LHTaskMethod.class))
                .map(method -> new LHTaskWorker(
                        executable,
                        method.getAnnotation(LHTaskMethod.class).value(),
                        getLittleHorseConfig()))
                .forEach(worker -> {
                    Runtime.getRuntime().addShutdownHook(new Thread(worker::close));
                    await(() -> {
                        worker.registerTaskDef();
                        worker.start();
                    });
                });
    }

    public Map<String, Object> getKafkaConfig() {
        return Map.of(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, getKafkaBootstrapServers());
    }

    public void createTopics(String... topics) {
        try {
            try (AdminClient adminClient = KafkaAdminClient.create(getKafkaConfig())) {
                List<NewTopic> topicList = Arrays.stream(topics)
                        .map(topic -> new NewTopic(topic, 1, (short) 1))
                        .collect(Collectors.toList());
                adminClient.createTopics(topicList).all().get(1, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            if (!(e.getCause() instanceof TopicExistsException)) {
                throw new RuntimeException(e);
            }
        }
    }

    public void produceValues(String topic, Pair<String, String>... keyValues) {
        try {
            Map<String, Object> kafkaConfig = new HashMap<>(getKafkaConfig());
            kafkaConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            kafkaConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

            try (Producer<String, String> producer = new KafkaProducer<>(kafkaConfig)) {
                Arrays.stream(keyValues)
                        .map(keyValue ->
                                new ProducerRecord<>(topic, keyValue.getKey(), keyValue.getValue()))
                        .forEach(record -> {
                            try {
                                producer.send(record).get(1, TimeUnit.SECONDS);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
