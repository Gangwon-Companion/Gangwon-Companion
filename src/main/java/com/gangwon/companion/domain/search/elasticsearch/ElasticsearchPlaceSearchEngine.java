package com.gangwon.companion.domain.search.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gangwon.companion.domain.search.dto.PlaceSearchRequest;
import com.gangwon.companion.domain.search.dto.PlaceSearchResponse;
import com.gangwon.companion.domain.search.service.PlaceSearchEngine;
import com.gangwon.companion.domain.search.service.SearchTraceRecorder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;

@Service
@ConditionalOnExpression("'${search.engine:elasticsearch}' == 'elasticsearch' or '${search.engine:elasticsearch}' == 'hybrid'")
public class ElasticsearchPlaceSearchEngine implements PlaceSearchEngine {
    private static final Map<String, List<String>> PREFERENCE_TERMS = Map.of(
            "quiet", List.of("조용", "한적"), "ocean_view", List.of("바다", "해변", "오션뷰"),
            "cafe", List.of("카페", "커피"), "oceanView", List.of("바다", "해변", "오션뷰"));

    private final ElasticsearchHttpClient client;
    private final ElasticsearchProperties properties;
    private final ObjectMapper objectMapper;
    private final EmbeddingProperties embeddingProperties;
    private final EmbeddingClient embeddingClient;

    public ElasticsearchPlaceSearchEngine(ElasticsearchHttpClient client, ElasticsearchProperties properties,
                                          ObjectMapper objectMapper) {
        this(client, properties, objectMapper, disabledEmbeddingProperties(), null);
    }

    @Autowired
    public ElasticsearchPlaceSearchEngine(ElasticsearchHttpClient client, ElasticsearchProperties properties,
                                          ObjectMapper objectMapper, EmbeddingProperties embeddingProperties,
                                          EmbeddingClient embeddingClient) {
        this.client = client;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.embeddingProperties = embeddingProperties;
        this.embeddingClient = embeddingClient;
    }

    private static EmbeddingProperties disabledEmbeddingProperties() {
        EmbeddingProperties value = new EmbeddingProperties();
        value.setEnabled(false);
        return value;
    }

    @Override
    public PlaceSearchResponse search(PlaceSearchRequest request) {
        JsonNode response;
        String path = "keyword";
        if (embeddingProperties.isEnabled() && !request.queryText().isBlank()) {
            try {
                response = client.post("/" + properties.getAlias() + "/_search", hybridBody(request));
                path = "hybrid_rrf";
            } catch (RuntimeException ignored) {
                response = client.post("/" + properties.getAlias() + "/_search", searchBody(request, false, false));
                path = "keyword_embedding_failed";
            }
        } else {
            response = client.post("/" + properties.getAlias() + "/_search", searchBody(request, false, false));
        }
        if (!request.queryText().isBlank() && response.path("hits").path("hits").isEmpty()) {
            response = client.post("/" + properties.getAlias() + "/_search", searchBody(request, true, false));
            path = "relaxed_keyword";
        }
        if (!request.queryText().isBlank() && response.path("hits").path("hits").isEmpty()) {
            response = client.post("/" + properties.getAlias() + "/_search", searchBody(request, true, true));
            path = "filter_only";
        }
        List<PlaceSearchResponse.Candidate> candidates = new ArrayList<>();
        for (JsonNode hit : response.path("hits").path("hits")) candidates.add(candidate(hit, request));
        return SearchTraceRecorder.capture(candidates.stream()
                .sorted(Comparator.comparingDouble(PlaceSearchResponse.Candidate::score).reversed()
                        .thenComparing(PlaceSearchResponse.Candidate::placeId))
                .limit(request.limit()).toList(), "elasticsearch", path);
    }

    Map<String, Object> hybridBody(PlaceSearchRequest request) {
        List<Double> vector = embeddingClient.embed(request.queryText(), "query");
        Map<String, Object> keyword = searchBody(request, false, false);
        Map<String, Object> functionScore = cast(keyword.get("query"));
        Map<String, Object> bool = cast(cast(functionScore.get("function_score")).get("query"));
        Map<String, Object> knn = new LinkedHashMap<>();
        knn.put("field", "embedding");
        knn.put("query_vector", vector);
        knn.put("k", Math.min(500, Math.max(50, request.limit() * 10)));
        knn.put("num_candidates", Math.min(1000, Math.max(100, request.limit() * 20)));
        Object filters = bool.getOrDefault("filter", List.of());
        knn.put("filter", Map.of("bool", Map.of("filter", filters)));
        Map<String, Object> retriever = Map.of("rrf", Map.of(
                "retrievers", List.of(Map.of("standard", Map.of("query", functionScore)), Map.of("knn", knn)),
                "rank_window_size", embeddingProperties.getRankWindow(),
                "rank_constant", embeddingProperties.getRankConstant()));
        return Map.of("size", keyword.get("size"), "_source", keyword.get("_source"),
                "retriever", retriever, "track_scores", true,
                "sort", List.of(Map.of("_score", "desc"), Map.of("placeId", "asc")));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> cast(Object value) {
        return (Map<String, Object>) value;
    }

    Map<String, Object> searchBody(PlaceSearchRequest request, boolean relaxed, boolean filterOnly) {
        List<Object> filters = new ArrayList<>();
        filters.add(Map.of("term", Map.of("domain", request.domain().name())));
        filters.add(Map.of("exists", Map.of("field", "opensAt")));
        filters.add(Map.of("exists", Map.of("field", "closesAt")));
        if (!request.regionCodes().isEmpty()) {
            filters.add(Map.of("terms", Map.of("regionCode", request.regionCodes().stream().map(Enum::name).toList())));
        }
        if (request.geo() != null) {
            filters.add(Map.of("geo_distance", Map.of("distance", request.geo().radiusKm() + "km",
                    "location", Map.of("lat", request.geo().center().lat(), "lon", request.geo().center().lon()))));
        }
        addNotFalseFilter(filters, "petAllowed", Boolean.TRUE.equals(request.hardFilters().petAllowed()));
        if (request.hardFilters().petSize() != null) {
            addNotFalseFilter(filters, switch (request.hardFilters().petSize()) {
                case SMALL -> "smallPetAllowed"; case MEDIUM -> "mediumPetAllowed"; case LARGE -> "largePetAllowed";
            }, true);
        }
        addNotFalseFilter(filters, "wheelchairAccessible", Boolean.TRUE.equals(request.hardFilters().wheelchairAccessible()));

        Map<String, Object> bool = new LinkedHashMap<>();
        bool.put("filter", filters);
        if (!request.queryText().isBlank() && !filterOnly) {
            Map<String, Object> multiMatch = new LinkedHashMap<>();
            multiMatch.put("query", request.queryText());
            multiMatch.put("operator", relaxed ? "or" : "and");
            if (relaxed) multiMatch.put("minimum_should_match", "70%");
            multiMatch.put("type", "cross_fields");
            multiMatch.put("fields", List.of(
                    "name^6", "name.english^3", "themeName^3", "menuType^3", "address^2",
                    "searchText^1.5", "searchText.english", "petInfoText^1.2", "accessibilityInfoText^1.2"));
            bool.put("must", List.of(Map.of("multi_match", multiMatch)));
            bool.put("should", keywordBoosts(request));
        } else if (!request.softPreferences().isEmpty()) {
            bool.put("should", preferenceBoosts(request));
        }
        int fetchSize = Math.min(500, Math.max(50, request.limit() * 10));
        Map<String, Object> functionScore = new LinkedHashMap<>();
        functionScore.put("query", Map.of("bool", bool));
        functionScore.put("functions", rankingFunctions(request));
        functionScore.put("score_mode", "sum");
        functionScore.put("boost_mode", "sum");
        return Map.of("size", fetchSize, "_source", Map.of("excludes", List.of("embedding")), "track_scores", true, "query", Map.of("function_score", functionScore),
                "sort", List.of(Map.of("_score", "desc"), Map.of("placeId", "asc")));
    }

    private void addNotFalseFilter(List<Object> filters, String field, boolean requested) {
        if (!requested) return;
        filters.add(Map.of("bool", Map.of("must_not", List.of(Map.of("term", Map.of(field, false))))));
    }

    private List<Object> rankingFunctions(PlaceSearchRequest request) {
        List<Object> functions = new ArrayList<>();
        functions.add(Map.of("field_value_factor", Map.of(
                "field", "rating", "factor", 0.2, "modifier", "sqrt", "missing", 0)));
        if (request.geo() != null) {
            functions.add(Map.of("gauss", Map.of("location", Map.of(
                    "origin", Map.of("lat", request.geo().center().lat(), "lon", request.geo().center().lon()),
                    "scale", Math.max(1.0, request.geo().radiusKm() / 2.0) + "km", "decay", 0.5)),
                    "weight", 0.5));
        }
        return functions;
    }

    private List<Object> keywordBoosts(PlaceSearchRequest request) {
        List<Object> boosts = new ArrayList<>();
        boosts.add(Map.of("term", Map.of("name.raw", Map.of("value", request.queryText(), "boost", 12.0))));
        boosts.add(Map.of("match_phrase", Map.of("name", Map.of("query", request.queryText(), "boost", 7.0))));
        boosts.addAll(preferenceBoosts(request));
        return boosts;
    }

    private List<Object> preferenceBoosts(PlaceSearchRequest request) {
        List<Object> boosts = new ArrayList<>();
        request.softPreferences().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            List<String> terms = PREFERENCE_TERMS.getOrDefault(entry.getKey(), List.of(entry.getKey()));
            for (String term : terms) {
                boosts.add(Map.of("match", Map.of("searchText", Map.of(
                        "query", term, "boost", Math.max(0.0, entry.getValue())))));
            }
        });
        return boosts;
    }

    PlaceSearchResponse.Candidate candidate(JsonNode hit, PlaceSearchRequest request) {
        try {
            PlaceSearchDocument doc = objectMapper.treeToValue(hit.path("_source"), PlaceSearchDocument.class);
            List<String> missing = missingFields(doc, request);
            List<PlaceSearchResponse.Evidence> evidence = evidence(doc, request);
            List<String> matched = matchedPreferences(doc.searchText(), request.softPreferences());
            double baseScore = hit.path("_score").isNumber() ? hit.path("_score").asDouble() : 0;
            Double distance = distance(request, doc.location());
            double score = Math.round((baseScore + distanceScore(distance, request)) * 1000.0) / 1000.0;
            return new PlaceSearchResponse.Candidate(doc.placeId(), PlaceSearchRequest.Domain.valueOf(doc.domain()),
                    doc.name(), doc.address(), doc.location() == null ? null
                    : new PlaceSearchResponse.Location(doc.location().lat(), doc.location().lon()), distance, score,
                    missing.isEmpty() ? PlaceSearchResponse.Status.OK : PlaceSearchResponse.Status.INSUFFICIENT_EVIDENCE,
                    missing, matched, evidence);
        } catch (Exception exception) {
            throw new ElasticsearchOperationException("Invalid search hit", exception);
        }
    }

    private List<String> missingFields(PlaceSearchDocument doc, PlaceSearchRequest request) {
        List<String> missing = new ArrayList<>();
        if (doc.opensAt() == null || doc.closesAt() == null) missing.add("operating_hours");
        if (Boolean.TRUE.equals(request.hardFilters().petAllowed()) && doc.petAllowed() == null) missing.add("pet_allowed");
        if (request.hardFilters().petSize() != null && petSize(doc, request.hardFilters().petSize()) == null) missing.add("pet_size");
        if (Boolean.TRUE.equals(request.hardFilters().wheelchairAccessible()) && doc.wheelchairAccessible() == null) {
            missing.add("wheelchair_accessible");
        }
        return missing;
    }

    private List<PlaceSearchResponse.Evidence> evidence(PlaceSearchDocument doc, PlaceSearchRequest request) {
        List<PlaceSearchResponse.Evidence> evidence = new ArrayList<>();
        if (doc.opensAt() != null && doc.closesAt() != null) {
            evidence.add(new PlaceSearchResponse.Evidence("opens_at", doc.opensAt(), doc.source()));
            evidence.add(new PlaceSearchResponse.Evidence("closes_at", doc.closesAt(), doc.source()));
        }
        if (Boolean.TRUE.equals(request.hardFilters().petAllowed()) && doc.petAllowed() != null) {
            evidence.add(new PlaceSearchResponse.Evidence("pet_allowed", doc.petAllowed(), doc.source()));
        }
        if (request.hardFilters().petSize() != null && petSize(doc, request.hardFilters().petSize()) != null) {
            evidence.add(new PlaceSearchResponse.Evidence("pet_size", petSize(doc, request.hardFilters().petSize()), doc.source()));
        }
        if (Boolean.TRUE.equals(request.hardFilters().wheelchairAccessible()) && doc.wheelchairAccessible() != null) {
            evidence.add(new PlaceSearchResponse.Evidence("wheelchair_accessible", doc.wheelchairAccessible(), doc.source()));
        }
        return evidence;
    }

    private Boolean petSize(PlaceSearchDocument doc, PlaceSearchRequest.PetSize size) {
        return switch (size) { case SMALL -> doc.smallPetAllowed(); case MEDIUM -> doc.mediumPetAllowed(); case LARGE -> doc.largePetAllowed(); };
    }

    private List<String> matchedPreferences(String text, Map<String, Double> preferences) {
        if (text == null || preferences == null) return List.of();
        return preferences.keySet().stream().filter(key -> PREFERENCE_TERMS.getOrDefault(key, List.of(key))
                .stream().anyMatch(text.toLowerCase()::contains)).sorted().toList();
    }

    private Double distance(PlaceSearchRequest request, PlaceSearchDocument.Location location) {
        if (request.geo() == null || location == null) return null;
        double lat1 = Math.toRadians(request.geo().center().lat());
        double lon1 = Math.toRadians(request.geo().center().lon());
        double lat2 = Math.toRadians(location.lat());
        double lon2 = Math.toRadians(location.lon());
        double a = Math.pow(Math.sin((lat2 - lat1) / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin((lon2 - lon1) / 2), 2);
        return Math.round(2 * 6371 * Math.asin(Math.sqrt(a)) * 100.0) / 100.0;
    }

    private double distanceScore(Double distance, PlaceSearchRequest request) {
        if (distance == null || request.geo() == null) return 0;
        return 0.2 * Math.max(0, 1 - distance / request.geo().radiusKm());
    }
}
