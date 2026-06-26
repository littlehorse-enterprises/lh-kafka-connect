package e2e.tests;

import static io.restassured.RestAssured.given;

import static org.hamcrest.CoreMatchers.hasItems;

import e2e.configs.E2ETest;

import org.junit.jupiter.api.Test;

import java.util.Map;

public class InstallPluginsTest extends E2ETest {

    private static Map<Object, Object> buildEntry(String className, String type) {
        return Map.of("class", className, "type", type, "version", System.getProperty("lhVersion"));
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
                                        "predicate"),
                                buildEntry(
                                        "io.littlehorse.connect.transform.JsonPathMapperTransform$Key",
                                        "transformation"),
                                buildEntry(
                                        "io.littlehorse.connect.transform.JsonPathMapperTransform$Value",
                                        "transformation"),
                                buildEntry(
                                        "io.littlehorse.connect.transform.JsonPathMapperTransform$Headers",
                                        "transformation"),
                                buildEntry(
                                        "io.littlehorse.connect.transform.LiteralMapperTransform$Key",
                                        "transformation"),
                                buildEntry(
                                        "io.littlehorse.connect.transform.LiteralMapperTransform$Value",
                                        "transformation"),
                                buildEntry(
                                        "io.littlehorse.connect.transform.LiteralMapperTransform$Headers",
                                        "transformation")));
    }
}
