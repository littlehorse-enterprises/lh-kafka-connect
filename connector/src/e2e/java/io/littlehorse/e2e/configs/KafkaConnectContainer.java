package io.littlehorse.e2e.configs;

import com.github.dockerjava.api.model.PortBinding;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.util.Objects;
import java.util.UUID;

public class KafkaConnectContainer extends GenericContainer<KafkaConnectContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME =
            DockerImageName.parse("confluentinc/cp-kafka-connect");
    private static final int DEFAULT_PORT = 8083;
    private static final String CONNECT_BOOTSTRAP_SERVERS = "CONNECT_BOOTSTRAP_SERVERS";
    private static final String CONNECT_GROUP_ID = "CONNECT_GROUP_ID";
    private static final String CONNECT_CONFIG_STORAGE_TOPIC = "CONNECT_CONFIG_STORAGE_TOPIC";
    private static final String CONNECT_OFFSET_STORAGE_TOPIC = "CONNECT_OFFSET_STORAGE_TOPIC";
    private static final String CONNECT_STATUS_STORAGE_TOPIC = "CONNECT_STATUS_STORAGE_TOPIC";
    private static final String CONNECT_KEY_CONVERTER = "CONNECT_KEY_CONVERTER";
    private static final String CONNECT_VALUE_CONVERTER = "CONNECT_VALUE_CONVERTER";
    private static final String CONNECT_REST_ADVERTISED_HOST_NAME =
            "CONNECT_REST_ADVERTISED_HOST_NAME";
    private static final String CONNECT_CONFIG_STORAGE_REPLICATION_FACTOR =
            "CONNECT_CONFIG_STORAGE_REPLICATION_FACTOR";
    private static final String CONNECT_OFFSET_STORAGE_REPLICATION_FACTOR =
            "CONNECT_OFFSET_STORAGE_REPLICATION_FACTOR";
    private static final String CONNECT_STATUS_STORAGE_REPLICATION_FACTOR =
            "CONNECT_STATUS_STORAGE_REPLICATION_FACTOR";
    private static final String DEFAULT_KAFKA_BOOTSTRAP_SERVERS = "kafka:19092";
    private static final int DEFAULT_ADVERTISED_PORT = 38083;
    public static final String CONNECT_REST_ADVERTISED_PORT = "CONNECT_REST_ADVERTISED_PORT";
    public static final String CONNECT_LISTENERS = "CONNECT_LISTENERS";
    public static final String CONNECT_REST_PORT = "CONNECT_REST_PORT";

    public KafkaConnectContainer(final DockerImageName image) {
        super(image);
        image.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        this.withExposedPorts(DEFAULT_PORT)
                .withKafkaBootstrapServers(DEFAULT_KAFKA_BOOTSTRAP_SERVERS)
                .withEnv(CONNECT_GROUP_ID, UUID.randomUUID().toString())
                .withEnv(CONNECT_CONFIG_STORAGE_TOPIC, "__connect_config")
                .withEnv(CONNECT_CONFIG_STORAGE_REPLICATION_FACTOR, "1")
                .withEnv(CONNECT_OFFSET_STORAGE_TOPIC, "__connect_offsets")
                .withEnv(CONNECT_OFFSET_STORAGE_REPLICATION_FACTOR, "1")
                .withEnv(CONNECT_STATUS_STORAGE_TOPIC, "__connect_status")
                .withEnv(CONNECT_STATUS_STORAGE_REPLICATION_FACTOR, "1")
                .withEnv(CONNECT_KEY_CONVERTER, "org.apache.kafka.connect.json.JsonConverter")
                .withEnv(CONNECT_VALUE_CONVERTER, "org.apache.kafka.connect.json.JsonConverter")
                .withEnv(CONNECT_REST_ADVERTISED_HOST_NAME, "localhost")
                .withEnv(CONNECT_LISTENERS, "http://0.0.0.0:" + DEFAULT_PORT)
                .withEnv(CONNECT_REST_PORT, String.valueOf(DEFAULT_PORT))
                .withAdvertisedPort(DEFAULT_ADVERTISED_PORT)
                .waitingFor(Wait.forLogMessage(".*\"GET /connectors HTTP/1.1\" 200.*", 1));
    }

    public KafkaConnectContainer withKafkaBootstrapServers(final String bootstrapServers) {
        return this.withEnv(CONNECT_BOOTSTRAP_SERVERS, bootstrapServers);
    }

    public String getUrl() {
        return String.format("http://%s:%s", getHost(), getMappedPort(DEFAULT_PORT));
    }

    public KafkaConnectContainer withPluginsFromHost(final String path) {
        return this.withCopyFileToContainer(
                MountableFile.forHostPath(path), "/usr/share/java/kafka-connect-container-plugins");
    }

    public KafkaConnectContainer withAdvertisedPort(final int port) {
        return this.withEnv(CONNECT_REST_ADVERTISED_PORT, String.valueOf(port))
                .withCreateContainerCmdModifier(cmd -> Objects.requireNonNull(cmd.getHostConfig())
                        .withPortBindings(
                                PortBinding.parse(String.format("%d:%d", port, DEFAULT_PORT))));
    }
}
