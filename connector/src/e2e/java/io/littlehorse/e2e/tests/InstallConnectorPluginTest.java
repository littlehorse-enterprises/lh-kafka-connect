package io.littlehorse.e2e.tests;

import static io.restassured.RestAssured.given;

import static org.hamcrest.CoreMatchers.hasItems;

import io.littlehorse.e2e.configs.KafkaConnectContainer;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.net.MalformedURLException;
import java.util.Map;

@Testcontainers
public class InstallConnectorPluginTest {

    public static final String VERSION = "7.8.0";
    public static final DockerImageName KAFKA_CONNECT_IMAGE =
            DockerImageName.parse("confluentinc/cp-kafka-connect").withTag(VERSION);
    public static final DockerImageName KAFKA_IMAGE =
            DockerImageName.parse("confluentinc/cp-kafka").withTag(VERSION);
    public static final String KAFKA_HOSTNAME = "kafka";
    public static final String BOOTSTRAP_SERVER = KAFKA_HOSTNAME + ":19092";
    public static final Network NETWORK = Network.newNetwork();

    @Container
    ConfluentKafkaContainer kafka = new ConfluentKafkaContainer(KAFKA_IMAGE)
            .withNetworkAliases(KAFKA_HOSTNAME)
            .withListener(BOOTSTRAP_SERVER)
            .withNetwork(NETWORK);

    @Container
    KafkaConnectContainer kafkaConnect = new KafkaConnectContainer(
                    KAFKA_CONNECT_IMAGE, BOOTSTRAP_SERVER)
            .dependsOn(kafka)
            .withNetwork(NETWORK)
            .withCopyFileToContainer(
                    MountableFile.forHostPath("build/bundle/lh-kafka-connect"),
                    "/usr/share/java/lh-kafka-connect");

    @Test
    public void shouldInstallLHKafkaConnectPlugin() throws MalformedURLException {
        Map<Object, Object> externalEventConnector = Map.of(
                "class",
                "io.littlehorse.connect.ExternalEventSinkConnector",
                "type",
                "sink",
                "version",
                "dev");
        Map<Object, Object> runWfConnector = Map.of(
                "class",
                "io.littlehorse.connect.WfRunSinkConnector",
                "type",
                "sink",
                "version",
                "dev");
        Map<Object, Object> predicateKey = Map.of(
                "class",
                "io.littlehorse.connect.predicate.FilterByFieldPredicate$Key",
                "type",
                "predicate",
                "version",
                "dev");
        Map<Object, Object> predicateValue = Map.of(
                "class",
                "io.littlehorse.connect.predicate.FilterByFieldPredicate$Value",
                "type",
                "predicate",
                "version",
                "dev");
        given().queryParams(Map.of("connectorsOnly", false))
                .when()
                .get(kafkaConnect.getUrl() + "/connector-plugins")
                .then()
                .statusCode(200)
                .assertThat()
                .body(
                        ".",
                        hasItems(
                                externalEventConnector,
                                runWfConnector,
                                predicateKey,
                                predicateValue));
    }
}
