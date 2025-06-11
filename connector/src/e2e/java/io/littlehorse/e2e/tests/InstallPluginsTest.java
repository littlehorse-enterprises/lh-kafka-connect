package io.littlehorse.e2e.tests;

import static io.restassured.RestAssured.given;

import static org.hamcrest.CoreMatchers.hasItems;

import io.littlehorse.e2e.configs.E2ETest;

import org.junit.jupiter.api.Test;

import java.util.Map;

public class InstallPluginsTest extends E2ETest {

    private static Map<Object, Object> buildEntry(String className, String type) {
        return Map.of(
                "class",
                className,
                "type",
                type,
                "version",
                System.getenv().getOrDefault("BUNDLE_VERSION", "dev"));
    }

    @Test
    public void shouldInstallLHKafkaConnectPlugin() {
        given().queryParams(Map.of("connectorsOnly", false))
                .when()
                .get(getKafkaConnectUrl("connector-plugins"))
                .then()
                .assertThat()
                .statusCode(200)
                .body(
                        ".",
                        hasItems(
                                buildEntry(
                                        "io.littlehorse.connect.ExternalEventSinkConnector",
                                        "sink"),
                                buildEntry(
                                        "io.littlehorse.connect.CorrelatedEventSinkConnector",
                                        "sink"),
                                buildEntry("io.littlehorse.connect.WfRunSinkConnector", "sink"),
                                buildEntry(
                                        "io.littlehorse.connect.predicate.FilterByFieldPredicate$Key",
                                        "predicate"),
                                buildEntry(
                                        "io.littlehorse.connect.predicate.FilterByFieldPredicate$Value",
                                        "predicate")));
    }
}
