package com.gangwon.companion.domain.search.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Request-local facts; the content version is not a source freshness timestamp. */
public record GroundingSnapshot(
        @JsonProperty("place_id") String placeId,
        String source,
        @JsonProperty("content_version") String contentVersion,
        Map<String, Object> facts,
        List<PlaceSearchResponse.Evidence> evidence,
        @JsonProperty("missing_fields") List<String> missingFields
) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public GroundingSnapshot {
        facts = java.util.Collections.unmodifiableMap(new TreeMap<>(facts));
        evidence = List.copyOf(evidence);
        missingFields = List.copyOf(missingFields);
    }

    public static GroundingSnapshot from(PlaceSearchResponse.Candidate candidate, String source) {
        Map<String, Object> facts = new TreeMap<>();
        facts.put("domain", candidate.domain().name());
        if (candidate.name() != null) facts.put("name", candidate.name());
        if (candidate.address() != null) facts.put("address", candidate.address());
        if (candidate.location() != null) facts.put("location", candidate.location());
        // Scores, matched preferences and estimated distances are not source facts.
        var evidence = candidate.evidence().stream()
                .sorted(java.util.Comparator.comparing(PlaceSearchResponse.Evidence::field)
                        .thenComparing(e -> String.valueOf(e.source()))
                        .thenComparing(e -> String.valueOf(e.value())))
                .toList();
        var missing = candidate.missingFields().stream().sorted().toList();
        try {
            byte[] content = MAPPER.writeValueAsBytes(List.of(candidate.placeId(), source, facts, evidence, missing));
            String version = "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
            return new GroundingSnapshot(candidate.placeId(), source, version, facts, evidence, missing);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot create grounding snapshot", exception);
        }
    }
}
