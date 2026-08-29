package com.gangwon.companion.domain.search.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class ElasticsearchHttpClient {
    private final ElasticsearchProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient client;

    public ElasticsearchHttpClient(ElasticsearchProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.client = HttpClient.newBuilder().connectTimeout(properties.getConnectTimeout()).build();
    }

    public JsonNode get(String path) { return request("GET", path, null, "application/json"); }
    public JsonNode put(String path, Object body) { return request("PUT", path, json(body), "application/json"); }
    public JsonNode post(String path) { return request("POST", path, null, "application/json"); }
    public JsonNode post(String path, Object body) { return request("POST", path, json(body), "application/json"); }
    public JsonNode postNdjson(String path, String body) { return request("POST", path, body, "application/x-ndjson"); }
    public JsonNode delete(String path) { return request("DELETE", path, null, "application/json"); }

    public boolean exists(String path) {
        HttpResponse<String> response = send("HEAD", path, null, "application/json");
        if (response.statusCode() == 200) return true;
        if (response.statusCode() == 404) return false;
        throw failure("HEAD", path, response);
    }

    private JsonNode request(String method, String path, String body, String contentType) {
        HttpResponse<String> response = send(method, path, body, contentType);
        if (response.statusCode() < 200 || response.statusCode() >= 300) throw failure(method, path, response);
        if (response.body() == null || response.body().isBlank()) return objectMapper.createObjectNode();
        try { return objectMapper.readTree(response.body()); }
        catch (IOException exception) { throw new ElasticsearchOperationException("Invalid Elasticsearch response", exception); }
    }

    private HttpResponse<String> send(String method, String path, String body, String contentType) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(properties.getUrl() + path))
                    .timeout(properties.getRequestTimeout()).header("Accept", "application/json");
            if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
                builder.header("Authorization", "ApiKey " + properties.getApiKey());
            }
            if (body == null) builder.method(method, HttpRequest.BodyPublishers.noBody());
            else builder.header("Content-Type", contentType).method(method, HttpRequest.BodyPublishers.ofString(body));
            return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ElasticsearchOperationException("Elasticsearch request interrupted", exception);
        } catch (IOException exception) {
            throw new ElasticsearchOperationException("Elasticsearch request failed", exception);
        }
    }

    private String json(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (IOException exception) { throw new ElasticsearchOperationException("Cannot serialize Elasticsearch request", exception); }
    }

    private ElasticsearchOperationException failure(String method, String path, HttpResponse<String> response) {
        return new ElasticsearchOperationException(method + " " + path + " returned " + response.statusCode() + ": " + response.body());
    }
}
