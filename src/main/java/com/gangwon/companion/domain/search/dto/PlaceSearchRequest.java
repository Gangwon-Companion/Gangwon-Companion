package com.gangwon.companion.domain.search.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record PlaceSearchRequest(
        Domain domain,
        String slot,
        @JsonProperty("region_codes") List<RegionCode> regionCodes,
        @JsonProperty("query_text") String queryText,
        @JsonProperty("hard_filters") HardFilters hardFilters,
        @JsonProperty("soft_preferences") Map<String, Double> softPreferences,
        GeoConstraint geo,
        Integer limit
) {
    public PlaceSearchRequest {
        Objects.requireNonNull(domain, "domain is required");
        if (slot == null || slot.isBlank()) throw new IllegalArgumentException("slot is required");
        regionCodes = regionCodes == null ? List.of() : List.copyOf(regionCodes);
        queryText = queryText == null ? "" : queryText;
        hardFilters = hardFilters == null ? new HardFilters(null, null, null) : hardFilters;
        softPreferences = softPreferences == null ? Map.of() : Map.copyOf(softPreferences);
        limit = limit == null ? 5 : limit;
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("limit must be between 1 and 100");
        if (hardFilters.petSize() != null && !Boolean.TRUE.equals(hardFilters.petAllowed())) {
            throw new IllegalArgumentException("pet_size requires pet_allowed=true");
        }
    }
    public enum Domain {
        DESTINATION,
        RESTAURANT,
        LODGING
    }

    public enum RegionCode {
        CHUNCHEON, WONJU, GANGNEUNG, DONGHAE, TAEBAEK, SOKCHO, SAMCHEOK,
        HONGCHEON, HOENGSEONG, YEONGWOL, PYEONGCHANG, JEONGSEON, CHEORWON,
        HWACHEON, YANGGU, INJE, GOSEONG, YANGYANG
    }

    public enum PetSize {
        SMALL,
        MEDIUM,
        LARGE
    }

    public record HardFilters(
            @JsonProperty("pet_allowed") Boolean petAllowed,
            @JsonProperty("pet_size") PetSize petSize,
            @JsonProperty("wheelchair_accessible") Boolean wheelchairAccessible
    ) {
    }

    public record GeoConstraint(
            GeoCenter center,
            @JsonProperty("radius_km") Double radiusKm
    ) {
        public GeoConstraint {
            Objects.requireNonNull(center, "geo.center is required");
            if (radiusKm == null || radiusKm <= 0 || radiusKm > 200) {
                throw new IllegalArgumentException("radius_km must be between 0 and 200");
            }
        }
    }

    public record GeoCenter(Double lat, Double lon) {
        public GeoCenter {
            if (lat == null || lat < -90 || lat > 90 || lon == null || lon < -180 || lon > 180) {
                throw new IllegalArgumentException("invalid geo center");
            }
        }
    }
}
