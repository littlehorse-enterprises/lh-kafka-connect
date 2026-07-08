package e2e.configs;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * An in-memory Apicurio Registry container used to back the JSON Schema converter e2e tests. The
 * registry exposes the v3 REST API used both by the tests (to register schemas) and by the
 * connector's converter (to resolve them).
 */
public class ApicurioRegistryContainer extends GenericContainer<ApicurioRegistryContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME =
            DockerImageName.parse("apicurio/apicurio-registry");
    public static final int PORT = 8080;

    public ApicurioRegistryContainer(final DockerImageName image) {
        super(image);
        image.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        this.withExposedPorts(PORT)
                .withEnv("QUARKUS_HTTP_PORT", String.valueOf(PORT))
                .waitingFor(Wait.forHttp("/apis/registry/v3/system/info")
                        .forPort(PORT)
                        .forStatusCode(200)
                        .withStartupTimeout(Duration.ofMinutes(3)));
    }

    /** External v3 API URL, reachable from the test JVM (host). */
    public String getUrl() {
        return String.format("http://%s:%s/apis/registry/v3", getHost(), getMappedPort(PORT));
    }
}
