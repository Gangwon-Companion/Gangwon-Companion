package com.gangwon.companion.domain.search.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PlaceSearchResponse(List<Candidate> results) {
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
    }

    public record Location(Double lat, Double lon) {
    }

    public record Evidence(String field, Object value, String source) {
    }
}
