package e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Validates the artifact produced by the {@code buildConfluentBundle} Gradle task (on which the
 * {@code e2e} task depends). It does not require any container, so it can be run on its own with
 * {@code ./gradlew :connector:e2e --tests "e2e.tests.ConfluentBundleTest"}.
 */
public class ConfluentBundleTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static Path bundleZip;

    @BeforeAll
    static void locateBundle() throws IOException {
        Path distDir = Path.of("build", "dist");
        assertThat(distDir)
                .as("dist directory produced by the buildConfluentBundle task")
                .isDirectory();
        try (Stream<Path> files = Files.list(distDir)) {
            bundleZip = files.filter(path -> path.getFileName().toString().endsWith(".zip"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "No bundle zip found in " + distDir.toAbsolutePath()));
        }
    }

    @Test
    public void shouldContainExpectedBundleStructure() throws IOException {
        List<String> entries = listEntries(bundleZip);

        assertThat(entries)
                .as("Confluent bundle structure")
                .contains(
                        "manifest.json",
                        "doc/README.md",
                        "doc/LICENSE.md",
                        "doc/CONFIGURATIONS.md",
                        "assets/logo.svg");

        assertThat(entries)
                .as("shaded connector jar under lib/")
                .anyMatch(entry -> entry.startsWith("lib/") && entry.endsWith(".jar"));
    }

    @Test
    public void shouldRelocateGrpcAndMergeServiceFilesInShadedJar() throws IOException {
        Path connectorJar = extractConnectorJar();
        try {
            List<String> jarEntries = listEntries(connectorJar);

            assertThat(jarEntries)
                    .as("io.grpc must be relocated to io.littlehorse.grpc")
                    .noneMatch(entry -> entry.startsWith("io/grpc/"))
                    .anyMatch(entry -> entry.startsWith("io/littlehorse/grpc/"));

            assertThat(jarEntries)
                    .as("merged service descriptors")
                    .contains(
                            "META-INF/services/org.apache.kafka.connect.sink.SinkConnector",
                            "META-INF/services/org.apache.kafka.connect.transforms.Transformation",
                            "META-INF/services/org.apache.kafka.connect.transforms.predicates.Predicate",
                            "META-INF/services/io.littlehorse.grpc.LoadBalancerProvider",
                            "META-INF/services/io.littlehorse.grpc.NameResolverProvider");
        } finally {
            Files.deleteIfExists(connectorJar);
        }
    }

    @Test
    public void shouldContainValidManifest() throws IOException {
        JsonNode manifest = readManifest();

        assertThat(manifest.path("name").asText()).isEqualTo("lh-kafka-connect");
        assertThat(manifest.path("version").asText()).isNotBlank();
        assertThat(manifest.path("title").asText()).isNotBlank();
        assertThat(manifest.path("owner").path("name").asText()).isNotBlank();
        assertThat(manifest.path("component_types"))
                .as("component_types")
                .anyMatch(type -> type.asText().equals("sink"));
    }

    private static JsonNode readManifest() throws IOException {
        try (ZipFile zipFile = new ZipFile(bundleZip.toFile())) {
            ZipEntry manifestEntry = zipFile.getEntry("manifest.json");
            assertThat(manifestEntry).as("manifest.json entry").isNotNull();
            try (InputStream in = zipFile.getInputStream(manifestEntry)) {
                return MAPPER.readTree(in);
            }
        }
    }

    private static Path extractConnectorJar() throws IOException {
        try (ZipFile zipFile = new ZipFile(bundleZip.toFile())) {
            ZipEntry jarEntry = zipFile.stream()
                    .filter(entry -> entry.getName().startsWith("lib/")
                            && entry.getName().endsWith(".jar"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No shaded jar found under lib/"));
            Path tempJar = Files.createTempFile("lh-kafka-connect", ".jar");
            try (InputStream in = zipFile.getInputStream(jarEntry)) {
                Files.copy(in, tempJar, StandardCopyOption.REPLACE_EXISTING);
            }
            return tempJar;
        }
    }

    private static List<String> listEntries(Path zip) throws IOException {
        try (ZipFile zipFile = new ZipFile(zip.toFile())) {
            return zipFile.stream().map(ZipEntry::getName).toList();
        }
    }
}
