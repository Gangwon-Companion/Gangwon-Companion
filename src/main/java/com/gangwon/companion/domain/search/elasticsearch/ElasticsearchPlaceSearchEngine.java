package com.gangwon.companion.domain.search.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gangwon.companion.domain.search.dto.PlaceSearchRequest;
import com.gangwon.companion.domain.search.dto.PlaceSearchResponse;
import com.gangwon.companion.domain.search.service.PlaceSearchEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "search.engine", havingValue = "elasticsearch")
public class ElasticsearchPlaceSearchEngine implements PlaceSearchEngine {
    private static final Map<String, List<String>> PREFERENCE_TERMS = Map.of(
            "quiet", List.of("조용", "한적"), "ocean_view", List.of("바다", "해변", "오션뷰"),
            "cafe", List.of("카페", "커피"), "oceanView", List.of("바다", "해변", "오션뷰"));

    private final ElasticsearchHttpClient client;
    private final ElasticsearchProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public PlaceSearchResponse search(PlaceSearchRequest request) {
        JsonNode response = client.post("/" + properties.getAlias() + "/_search", searchBody(request));
        List<PlaceSearchResponse.Candidate> candidates = new ArrayList<>();
        for (JsonNode hit : response.path("hits").path("hits")) candidates.add(candidate(hit, request));
        return new PlaceSearchResponse(candidates.stream()
                .sorted(Comparator.comparingDouble(PlaceSearchResponse.Candidate::score).reversed()
                        .thenComparing(PlaceSearchResponse.Candidate::placeId))
                .limit(request.limit()).toList());
    }

    private Map<String, Object> searchBody(PlaceSearchRequest request) {
        List<Object> filters = new ArrayList<>();
        filters.add(Map.of("term", Map.of("domain", request.domain().name())));
        if (!request.regionCodes().isEmpty()) {
            filters.add(Map.of("terms", Map.of("regionCode", request.regionCodes().stream().map(Enum::name).toList())));
        }
        if (request.geo() != null) {
            filters.add(Map.of("geo_distance", Map.of("distance", request.geo().radiusKm() + "km",
                    "location", Map.of("lat", request.geo().center().lat(), "lon", request.geo().center().lon()))));
        }
        addNullableTrueFilter(filters, "petAllowed", Boolean.TRUE.equals(request.hardFilters().petAllowed()));
        if (request.hardFilters().petSize() != null) {
            addNullableTrueFilter(filters, switch (request.hardFilters().petSize()) {
                case SMALL -> "smallPetAllowed"; case MEDIUM -> "mediumPetAllowed"; case LARGE -> "largePetAllowed";
            }, true);
        }
        addNullableTrueFilter(filters, "wheelchairAccessible", Boolean.TRUE.equals(request.hardFilters().wheelchairAccessible()));

        Map<String, Object> bool = new LinkedHashMap<>();
        bool.put("filter", filters);
        if (!request.queryText().isBlank()) {
            bool.put("must", List.of(Map.of("multi_match", Map.of("query", request.queryText(), "operator", "or",
                    "fields", List.of("name^3", "name.english^2", "searchText", "searchText.english", "address")))));
        }
        int fetchSize = Math.min(500, Math.max(50, request.limit() * 10));
        return Map.of("size", fetchSize, "track_scores", true, "query", Map.of("bool", bool),
                "sort", List.of(Map.of("_score", "desc"), Map.of("placeId", "asc")));
    }

    private void addNullableTrueFilter(List<Object> filters, String field, boolean requested) {
        if (!requested) return;
        filters.add(Map.of("bool", Map.of("minimum_should_match", 1, "should", List.of(
                Map.of("term", Map.of(field, true)),
                Map.of("bool", Map.of("must_not", Map.of("exists", Map.of("field", field))))))));
    }

    private PlaceSearchResponse.Candidate candidate(JsonNode hit, PlaceSearchRequest request) {
        try {
            PlaceSearchDocument doc = objectMapper.treeToValue(hit.path("_source"), PlaceSearchDocument.class);
            List<String> missing = missingFields(doc, request);
            List<PlaceSearchResponse.Evidence> evidence = evidence(doc, request);
            List<String> matched = matchedPreferences(doc.searchText(), request.softPreferences());
            double baseScore = hit.path("_score").isNumber() ? hit.path("_score").asDouble() : 0;
            Double distance = distance(request, doc.location());
            double score = Math.round((baseScore + preferenceScore(matched, request.softPreferences())
                    + distanceScore(distance, request)) * 1000.0) / 1000.0;
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
        if (Boolean.TRUE.equals(request.hardFilters().petAllowed()) && doc.petAllowed() == null) missing.add("pet_allowed");
        if (request.hardFilters().petSize() != null && petSize(doc, request.hardFilters().petSize()) == null) missing.add("pet_size");
        if (Boolean.TRUE.equals(request.hardFilters().wheelchairAccessible()) && doc.wheelchairAccessible() == null) {
            missing.add("wheelchair_accessible");
        }
        return missing;
    }

    private List<PlaceSearchResponse.Evidence> evidence(PlaceSearchDocument doc, PlaceSearchRequest request) {
        List<PlaceSearchResponse.Evidence> evidence = new ArrayList<>();
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

    private double preferenceScore(List<String> matched, Map<String, Double> preferences) {
        return matched.stream().mapToDouble(key -> preferences.getOrDefault(key, 0.0) * 0.2).sum();
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
