package com.gangwon.companion.domain.search.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class EmbeddingClient {
    private final EmbeddingProperties properties;
    private final ObjectMapper mapper;
    private final HttpClient client;

    public EmbeddingClient(EmbeddingProperties properties, ObjectMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
        this.client = HttpClient.newBuilder().connectTimeout(properties.getTimeout()).build();
    }

    public List<Double> embed(String text, String kind) {
        if (!properties.isEnabled()) throw new IllegalStateException("embedding_disabled");
        try {
            var request = HttpRequest.newBuilder(URI.create(properties.getUrl() + "/embed"))
                    .timeout(properties.getTimeout()).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(
                            Map.of("texts", List.of(text), "kind", kind)))).build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new IllegalStateException("embedding_http_" + response.statusCode());
            var body = mapper.readTree(response.body());
            if (!properties.getModel().equals(body.path("model").asText())
                    || !properties.getRevision().equals(body.path("revision").asText())
                    || properties.getDimensions() != body.path("dimensions").asInt()
                    || !"l2".equals(body.path("normalization").asText())) {
                throw new IllegalStateException("embedding_model_mismatch");
            }
            var values = body.path("vectors").path(0);
            if (values.size() != properties.getDimensions()) throw new IllegalStateException("embedding_dimension_mismatch");
            List<Double> vector = new ArrayList<>();
            double norm = 0;
            for (var value : values) {
                if (!value.isNumber() || !Double.isFinite(value.asDouble())) throw new IllegalStateException("invalid_embedding");
                vector.add(value.asDouble());
                norm += value.asDouble() * value.asDouble();
            }
            if (Math.abs(norm - 1.0) > 0.01) throw new IllegalStateException("embedding_not_normalized");
            return List.copyOf(vector);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("embedding_interrupted", exception);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("embedding_unavailable", exception);
        }
    }
}
