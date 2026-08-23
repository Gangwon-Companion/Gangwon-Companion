package com.gangwon.companion.domain.search.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ElasticsearchIndexService {
    private final ElasticsearchHttpClient client;
    private final ElasticsearchProperties properties;
    private final PlaceSearchDocumentAssembler assembler;
    private final ObjectMapper objectMapper;
    private final Clock clock = Clock.systemUTC();

    public ReindexReport reindex() {
        String index = properties.getIndexPrefix() + "-" + DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")
                .withZone(java.time.ZoneOffset.UTC).format(clock.instant());
        client.put("/" + index, indexDefinition());
        List<PlaceSearchDocument> documents = assembler.loadAll();
        List<String> failedIds = bulkIndex(index, documents);
        List<String> retriedIds = List.copyOf(failedIds);
        int retryCount = failedIds.isEmpty() ? 0 : 1;
        if (!failedIds.isEmpty()) {
            Map<String, PlaceSearchDocument> byId = documents.stream()
                    .collect(java.util.stream.Collectors.toMap(PlaceSearchDocument::placeId, value -> value));
            failedIds = bulkIndex(index, failedIds.stream().map(byId::get).filter(java.util.Objects::nonNull).toList());
        }
        if (!failedIds.isEmpty()) return new ReindexReport(index, documents.size(), 0, failedIds, retriedIds, false, retryCount);
        client.post("/" + index + "/_refresh");
        long indexed = client.get("/" + index + "/_count").path("count").asLong();
        if (indexed != documents.size()) {
            throw new ElasticsearchOperationException("Indexed count mismatch: expected=" + documents.size() + ", actual=" + indexed);
        }
        switchAlias(index);
        return new ReindexReport(index, documents.size(), indexed, List.of(), retriedIds, true, retryCount);
    }

    private List<String> bulkIndex(String index, List<PlaceSearchDocument> documents) {
        List<String> failures = new ArrayList<>();
        for (int start = 0; start < documents.size(); start += properties.getBulkSize()) {
            List<PlaceSearchDocument> batch = documents.subList(start, Math.min(documents.size(), start + properties.getBulkSize()));
            StringBuilder ndjson = new StringBuilder();
            for (PlaceSearchDocument document : batch) {
                ndjson.append(json(Map.of("index", Map.of("_id", document.placeId())))).append('\n');
                ndjson.append(json(document)).append('\n');
            }
            JsonNode response = client.postNdjson("/" + index + "/_bulk", ndjson.toString());
            if (response.path("errors").asBoolean()) {
                for (JsonNode item : response.path("items")) {
                    JsonNode result = item.path("index");
                    if (result.has("error")) failures.add(result.path("_id").asText());
                }
            }
        }
        return failures;
    }

    private void switchAlias(String newIndex) {
        List<Map<String, Object>> actions = new ArrayList<>();
        String alias = properties.getAlias();
        if (client.exists("/_alias/" + alias)) {
            JsonNode aliases = client.get("/_alias/" + alias);
            aliases.fieldNames().forEachRemaining(index -> actions.add(Map.of("remove", Map.of("index", index, "alias", alias))));
        }
        actions.add(Map.of("add", Map.of("index", newIndex, "alias", alias, "is_write_index", true)));
        client.post("/_aliases", Map.of("actions", actions));
    }

    private Map<String, Object> indexDefinition() {
        Map<String, Object> text = Map.of("type", "text", "analyzer", "gangwon_korean",
                "fields", Map.of(
                        "raw", Map.of("type", "keyword", "normalizer", "lowercase_normalizer"),
                        "english", Map.of("type", "text", "analyzer", "standard")));
        Map<String, Object> propertiesMap = new LinkedHashMap<>();
        propertiesMap.put("placeId", Map.of("type", "keyword"));
        propertiesMap.put("domain", Map.of("type", "keyword"));
        propertiesMap.put("name", text);
        propertiesMap.put("address", text);
        propertiesMap.put("regionCode", Map.of("type", "keyword"));
        propertiesMap.put("searchText", text);
        propertiesMap.put("location", Map.of("type", "geo_point"));
        for (String field : List.of("petAllowed", "smallPetAllowed", "mediumPetAllowed", "largePetAllowed", "wheelchairAccessible")) {
            propertiesMap.put(field, Map.of("type", "boolean"));
        }
        propertiesMap.put("source", Map.of("type", "keyword"));
        propertiesMap.put("evidenceFields", Map.of("type", "keyword"));
        propertiesMap.put("embedding", Map.of("type", "dense_vector", "similarity", "cosine"));
        Map<String, Object> analysis = Map.of(
                "analyzer", Map.of("gangwon_korean",
                        Map.of("type", "custom", "tokenizer", "nori_tokenizer", "filter", List.of("lowercase", "nori_readingform"))),
                "normalizer", Map.of("lowercase_normalizer",
                        Map.of("type", "custom", "filter", List.of("lowercase"))));
        return Map.of("settings", Map.of("number_of_shards", 1, "number_of_replicas", 0, "analysis", analysis),
                "mappings", Map.of("dynamic", "strict", "properties", propertiesMap));
    }

    private String json(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (Exception exception) { throw new ElasticsearchOperationException("Cannot serialize bulk document", exception); }
    }

    public record ReindexReport(String index, long sourceCount, long indexedCount, List<String> failedIds,
                                List<String> retriedIds, boolean aliasSwitched, int retryCount) {}
}
