package io.littlehorse.e2e.tests;

import static io.restassured.RestAssured.given;

import static org.hamcrest.CoreMatchers.hasItems;

import io.littlehorse.e2e.configs.E2ETest;

import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.util.Map;

public class InstallPluginsTest extends E2ETest {

    private static Map<Object, Object> buildPluginEntry(String className, String type) {
        return Map.of(
                "class",
                className,
                "type",
                type,
                "version",
                System.getenv().getOrDefault("BUNDLE_VERSION", "dev"));
    }

    @Test
    public void shouldInstallLHKafkaConnectPlugin() throws MalformedURLException {
        Map<Object, Object> externalEventConnector =
                buildPluginEntry("io.littlehorse.connect.ExternalEventSinkConnector", "sink");
        Map<Object, Object> runWfConnector =
                buildPluginEntry("io.littlehorse.connect.WfRunSinkConnector", "sink");
        Map<Object, Object> predicateKey = buildPluginEntry(
                "io.littlehorse.connect.predicate.FilterByFieldPredicate$Key", "predicate");
        Map<Object, Object> predicateValue = buildPluginEntry(
                "io.littlehorse.connect.predicate.FilterByFieldPredicate$Value", "predicate");
        given().queryParams(Map.of("connectorsOnly", false))
                .when()
                .get(getKafkaConnectUrl("connector-plugins"))
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
