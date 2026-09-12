package com.gangwon.companion.domain.search.elasticsearch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EmbeddingDocumentService {
    private final EmbeddingProperties properties;
    private final EmbeddingClient client;
    private final ObjectMapper mapper;

    public boolean enabled() { return properties.isEnabled(); }
    public int dimensions() { return properties.getDimensions(); }

    public Map<String, Object> enrich(PlaceSearchDocument document, JsonNode previous) {
        Map<String, Object> result = new LinkedHashMap<>(mapper.convertValue(document, new TypeReference<Map<String, Object>>() {}));
        if (!enabled()) return result;
        // One place per vector. E5 truncates to 512 tokens; full source text remains in the index.
        String text = document.searchText() == null || document.searchText().isBlank() ? document.name() : document.searchText();
        if (text == null || text.isBlank()) text = document.placeId();
        if (text.length() > 16000) text = text.substring(0, 16000);
        String hash = hash(text);
        result.put("embeddingHash", hash);
        result.put("embeddingModel", properties.getModel());
        result.put("embeddingRevision", properties.getRevision());
        result.put("embeddingDimensions", dimensions());
        if (previous != null && "ready".equals(previous.path("embeddingStatus").asText())
                && hash.equals(previous.path("embeddingHash").asText())
                && properties.getModel().equals(previous.path("embeddingModel").asText())
                && properties.getRevision().equals(previous.path("embeddingRevision").asText())
                && dimensions() == previous.path("embeddingDimensions").asInt()
                && previous.path("embedding").size() == dimensions()) {
            result.put("embedding", mapper.convertValue(previous.path("embedding"), Object.class));
            result.put("embeddingCreatedAt", previous.path("embeddingCreatedAt").asText());
            result.put("embeddingStatus", "ready");
            return result;
        }
        try {
            result.put("embedding", client.embed(text, "passage"));
            result.put("embeddingCreatedAt", Instant.now().toString());
            result.put("embeddingStatus", "ready");
        } catch (RuntimeException exception) {
            // Never carry an old vector onto changed text. Keyword search remains available.
            result.put("embeddingStatus", "failed");
            result.put("embeddingFailure", exception.getClass().getSimpleName());
        }
        return result;
    }

    private String hash(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
}
