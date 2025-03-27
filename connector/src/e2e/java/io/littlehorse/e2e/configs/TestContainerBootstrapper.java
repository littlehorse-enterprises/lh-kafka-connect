package io.littlehorse.e2e.configs;

import io.littlehorse.container.LittleHorseContainer;
import io.littlehorse.sdk.common.config.LHConfig;
import io.littlehorse.test.internal.TestBootstrapper;

import org.testcontainers.containers.Network;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

public class TestContainerBootstrapper implements TestBootstrapper {

    public static final String CONFLUENT_VERSION = "7.8.0";
    public static final String LH_VERSION = "0.13.0";
    public static final DockerImageName KAFKA_CONNECT_IMAGE =
            DockerImageName.parse("confluentinc/cp-kafka-connect").withTag(CONFLUENT_VERSION);
    public static final DockerImageName KAFKA_IMAGE =
            DockerImageName.parse("confluentinc/cp-kafka").withTag(CONFLUENT_VERSION);
    public static final DockerImageName LH_IMAGE = DockerImageName.parse(
                    "ghcr.io/littlehorse-enterprises/littlehorse/lh-server")
            .withTag(LH_VERSION);
    public static final String KAFKA_HOSTNAME = "kafka";
    public static final String KAFKA_BOOTSTRAP_SERVER = KAFKA_HOSTNAME + ":19092";
    public static final Network NETWORK = Network.newNetwork();

    public static final ConfluentKafkaContainer KAFKA = new ConfluentKafkaContainer(KAFKA_IMAGE)
            .withNetworkAliases(KAFKA_HOSTNAME)
            .withListener(KAFKA_BOOTSTRAP_SERVER)
            .withNetwork(NETWORK);

    public static final KafkaConnectContainer KAFKA_CONNECT = new KafkaConnectContainer(
                    KAFKA_CONNECT_IMAGE)
            .dependsOn(KAFKA)
            .withKafkaBootstrapServers(KAFKA_BOOTSTRAP_SERVER)
            .withNetwork(NETWORK)
            .withPluginsFromHost("./build/bundle");

    public static final LittleHorseContainer LITTLEHORSE = new LittleHorseContainer(LH_IMAGE)
            .dependsOn(KAFKA, KAFKA_CONNECT)
            .withKafkaBootstrapServers(KAFKA_BOOTSTRAP_SERVER)
            .withNetwork(NETWORK);

    public TestContainerBootstrapper() {
        LITTLEHORSE.start();
    }

    @Override
    public LHConfig getWorkerConfig() {
        return new LHConfig(LITTLEHORSE.getClientProperties());
    }
}
