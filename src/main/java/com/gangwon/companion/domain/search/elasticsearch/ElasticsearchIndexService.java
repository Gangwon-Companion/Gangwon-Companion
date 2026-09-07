package com.gangwon.companion.domain.search.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ElasticsearchIndexService {
    private final ElasticsearchHttpClient client;
    private final ElasticsearchProperties properties;
    private final PlaceSearchDocumentAssembler assembler;
    private final ObjectMapper objectMapper;
    private final EmbeddingDocumentService embeddings;
    private final Clock clock = Clock.systemUTC();

    public ElasticsearchIndexService(ElasticsearchHttpClient client, ElasticsearchProperties properties,
                                     PlaceSearchDocumentAssembler assembler, ObjectMapper objectMapper) {
        this(client, properties, assembler, objectMapper, disabledEmbeddings(objectMapper));
    }

    @Autowired
    public ElasticsearchIndexService(ElasticsearchHttpClient client, ElasticsearchProperties properties,
                                     PlaceSearchDocumentAssembler assembler, ObjectMapper objectMapper,
                                     EmbeddingDocumentService embeddings) {
        this.client = client;
        this.properties = properties;
        this.assembler = assembler;
        this.objectMapper = objectMapper;
        this.embeddings = embeddings;
    }

    private static EmbeddingDocumentService disabledEmbeddings(ObjectMapper mapper) {
        EmbeddingProperties props = new EmbeddingProperties();
        props.setEnabled(false);
        return new EmbeddingDocumentService(props, null, mapper);
    }

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

    public synchronized void upsert(PlaceSearchDocument document) {
        String path = "/" + properties.getAlias() + "/_doc/" + document.placeId();
        if (!embeddings.enabled()) {
            client.put(path, document);
            return;
        }
        JsonNode previous = client.exists(path) ? client.get(path) : null;
        var enriched = embeddings.enrich(document, previous == null ? null : previous.path("_source"));
        String[] identity = document.placeId().split(":", 2);
        var current = assembler.loadOne(identity[0], Long.parseLong(identity[1]));
        if (current.isEmpty()) { delete(document.placeId()); return; }
        if (!current.get().equals(document)) throw new ElasticsearchOperationException("Source changed during embedding; retry latest aggregate");
        // Optimistic concurrency prevents stale work overwriting another indexer's update.
        String condition = previous == null ? "?op_type=create" : "?if_seq_no=" + previous.path("_seq_no").asLong()
                + "&if_primary_term=" + previous.path("_primary_term").asLong();
        client.put(path + condition, enriched);
    }

    public synchronized void delete(String placeId) {
        if (client.exists("/" + properties.getAlias() + "/_doc/" + placeId)) {
            client.delete("/" + properties.getAlias() + "/_doc/" + placeId);
        }
    }

    private List<String> bulkIndex(String index, List<PlaceSearchDocument> documents) {
        List<String> failures = new ArrayList<>();
        for (int start = 0; start < documents.size(); start += properties.getBulkSize()) {
            List<PlaceSearchDocument> batch = documents.subList(start, Math.min(documents.size(), start + properties.getBulkSize()));
            StringBuilder ndjson = new StringBuilder();
            for (PlaceSearchDocument document : batch) {
                ndjson.append(json(Map.of("index", Map.of("_id", document.placeId())))).append('\n');
                ndjson.append(json(embeddings.enrich(document, null))).append('\n');
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
        propertiesMap.put("themeName", text);
        propertiesMap.put("menuType", text);
        propertiesMap.put("rating", Map.of("type", "double"));
        propertiesMap.put("price", Map.of("type", "long"));
        propertiesMap.put("petInfoText", text);
        propertiesMap.put("accessibilityInfoText", text);
        propertiesMap.put("opensAt", Map.of("type", "keyword"));
        propertiesMap.put("closesAt", Map.of("type", "keyword"));
        propertiesMap.put("operatingHoursRaw", Map.of("type", "text", "index", false));
        propertiesMap.put("updatedAt", Map.of("type", "date"));
        propertiesMap.put("documentVersion", Map.of("type", "integer"));
        propertiesMap.put("source", Map.of("type", "keyword"));
        propertiesMap.put("evidenceFields", Map.of("type", "keyword"));
        propertiesMap.put("embedding", Map.of("type", "dense_vector", "dims", embeddings.dimensions(),
                "index", true, "similarity", "cosine", "index_options", Map.of("type", "int8_hnsw")));
        for (String field : List.of("embeddingHash", "embeddingModel", "embeddingRevision", "embeddingStatus", "embeddingFailure")) {
            propertiesMap.put(field, Map.of("type", "keyword"));
        }
        propertiesMap.put("embeddingDimensions", Map.of("type", "integer"));
        propertiesMap.put("embeddingCreatedAt", Map.of("type", "date"));
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
