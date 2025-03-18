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
    public static final String BUNDLE_VERSION = "BUNDLE_VERSION";

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

    private static Map<Object, Object> buildEntry(String className, String type) {
        return Map.of(
                "class",
                className,
                "type",
                type,
                "version",
                System.getenv().getOrDefault(BUNDLE_VERSION, "dev"));
    }

    @Test
    public void shouldInstallLHKafkaConnectPlugin() throws MalformedURLException {
        Map<Object, Object> externalEventConnector =
                buildEntry("io.littlehorse.connect.ExternalEventSinkConnector", "sink");
        Map<Object, Object> runWfConnector =
                buildEntry("io.littlehorse.connect.WfRunSinkConnector", "sink");
        Map<Object, Object> predicateKey = buildEntry(
                "io.littlehorse.connect.predicate.FilterByFieldPredicate$Key", "predicate");
        Map<Object, Object> predicateValue = buildEntry(
                "io.littlehorse.connect.predicate.FilterByFieldPredicate$Value", "predicate");
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
