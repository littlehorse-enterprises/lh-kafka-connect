package io.littlehorse.e2e.tests;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.hasItem;

import io.littlehorse.e2e.configs.KafkaConnectContainer;
import java.net.MalformedURLException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@Testcontainers
public class ExtractFieldsTests {

    public static final String VERSION = "7.8.0";
    public static final DockerImageName KAFKA_CONNECT_IMAGE = DockerImageName
        .parse("confluentinc/cp-kafka-connect")
        .withTag(VERSION);
    public static final DockerImageName KAFKA_IMAGE = DockerImageName
        .parse("confluentinc/cp-kafka")
        .withTag(VERSION);
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
        KAFKA_CONNECT_IMAGE,
        BOOTSTRAP_SERVER
    )
        .dependsOn(kafka)
        .withNetwork(NETWORK)
        .withCopyFileToContainer(
            MountableFile.forHostPath("build/bundle/lh-kafka-connect"),
            "/usr/share/java/lh-kafka-connect"
        );

    @Test
    public void shouldInstallLHKafkaConnectPlugin()
        throws MalformedURLException {
        Map<Object, Object> plugin = Map.of(
            "class",
            "io.littlehorse.kafka.connect.LHSinkConnector",
            "type",
            "sink",
            "version",
            "dev"
        );
        when()
            .get(kafkaConnect.getUrl() + "/connector-plugins")
            .then()
            .statusCode(200)
            .assertThat()
            .body(".", hasItem(plugin));
    }
}
