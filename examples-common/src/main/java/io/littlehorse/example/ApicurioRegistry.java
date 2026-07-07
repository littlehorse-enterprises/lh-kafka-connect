package io.littlehorse.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

/**
 * Minimal helper to register JSON Schema artifacts (optionally with references) in an Apicurio
 * Registry v3 instance. Used by the examples to register schemas before producing records.
 */
public class ApicurioRegistry {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String baseUrl;
    private final HttpClient http = HttpClient.newHttpClient();

    /** @param baseUrl the v3 API base, e.g. {@code http://localhost:8080/apis/registry/v3}. */
    public ApicurioRegistry(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void register(String groupId, String artifactId, String schema) {
        register(groupId, artifactId, schema, List.of());
    }

    public void register(
            String groupId, String artifactId, String schema, List<Reference> references) {
        try {
            ObjectNode content = MAPPER.createObjectNode();
            content.put("content", schema);
            content.put("contentType", "application/json");
            if (!references.isEmpty()) {
                ArrayNode refs = content.putArray("references");
                for (Reference reference : references) {
                    ObjectNode ref = refs.addObject();
                    ref.put("name", reference.name());
                    ref.put("groupId", reference.groupId());
                    ref.put("artifactId", reference.artifactId());
                    ref.put("version", reference.version());
                }
            }

            ObjectNode firstVersion = MAPPER.createObjectNode();
            firstVersion.set("content", content);

            ObjectNode body = MAPPER.createObjectNode();
            body.put("artifactId", artifactId);
            body.put("artifactType", "JSON");
            body.set("firstVersion", firstVersion);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/groups/" + groupId
                            + "/artifacts?ifExists=FIND_OR_CREATE_VERSION"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response =
                    http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new RuntimeException("Failed to register artifact '" + artifactId + "': "
                        + response.statusCode() + " " + response.body());
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** A JSON Schema artifact reference: maps a {@code $ref} name to a registered artifact. */
    public record Reference(String name, String groupId, String artifactId, String version) {}
}
