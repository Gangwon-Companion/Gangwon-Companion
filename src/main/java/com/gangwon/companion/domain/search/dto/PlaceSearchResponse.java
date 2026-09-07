package com.gangwon.companion.domain.search.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PlaceSearchResponse(List<Candidate> results, SearchTrace trace) {
    public PlaceSearchResponse(List<Candidate> results) {
        this(results, null);
    }

    public record SearchTrace(
            @JsonProperty("schema_version") int schemaVersion,
            @JsonProperty("request_id") String requestId,
            @JsonProperty("retrieved_at") String retrievedAt,
            String engine, String path,
            List<GroundingSnapshot> snapshots
    ) {}
    public enum Status {
        OK,
        INSUFFICIENT_EVIDENCE
    }

    public record Candidate(
            @JsonProperty("place_id") String placeId,
            PlaceSearchRequest.Domain domain,
            String name,
            String address,
            Location location,
            @JsonProperty("distance_km") Double distanceKm,
            Double score,
            Status status,
            @JsonProperty("missing_fields") List<String> missingFields,
            @JsonProperty("matched_preferences") List<String> matchedPreferences,
            List<Evidence> evidence
    ) {
        public Candidate {
            missingFields = List.copyOf(missingFields);
            matchedPreferences = List.copyOf(matchedPreferences);
            evidence = List.copyOf(evidence);
        }
    }

    public record Location(Double lat, Double lon) {
    }

    public record Evidence(String field, Object value, String source) {
    }
}
