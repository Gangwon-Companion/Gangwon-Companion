package com.gangwon.companion.domain.search.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

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
    }

    public record GeoCenter(Double lat, Double lon) {
    }
}
