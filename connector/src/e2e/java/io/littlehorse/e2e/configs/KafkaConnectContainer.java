package io.littlehorse.e2e.configs;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
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
    private static final String CONNECT_REST_PORT = "CONNECT_REST_PORT";
    private static final String CONNECT_PLUGIN_PATH = "CONNECT_PLUGIN_PATH";
    private static final String CONNECT_CONFIG_STORAGE_REPLICATION_FACTOR =
            "CONNECT_CONFIG_STORAGE_REPLICATION_FACTOR";
    private static final String CONNECT_OFFSET_STORAGE_REPLICATION_FACTOR =
            "CONNECT_OFFSET_STORAGE_REPLICATION_FACTOR";
    private static final String CONNECT_STATUS_STORAGE_REPLICATION_FACTOR =
            "CONNECT_STATUS_STORAGE_REPLICATION_FACTOR";

    public KafkaConnectContainer(final DockerImageName image, final String bootstrapServers) {
        super(image);
        image.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        this.withExposedPorts(DEFAULT_PORT)
                .withEnv(CONNECT_BOOTSTRAP_SERVERS, bootstrapServers)
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
                .withEnv(CONNECT_REST_PORT, String.valueOf(DEFAULT_PORT))
                .waitingFor(Wait.forLogMessage(".*\"GET /connectors HTTP/1.1\" 200.*", 1));
    }

    public URL getUrl() throws MalformedURLException {
        return URI.create(String.format("http://%s:%s", getHost(), getMappedPort(DEFAULT_PORT)))
                .toURL();
    }
}
