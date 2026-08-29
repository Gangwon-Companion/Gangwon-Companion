package com.gangwon.companion.domain.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CourseRecommendationRequest(
        @NotBlank @Size(max = 1000) String message,
        String region,
        @JsonProperty("travel_days") @Min(1) @Max(30) Integer travelDays,
        @Min(0) @Max(29) Integer nights,
        @JsonProperty("pet_allowed") Boolean petAllowed,
        @JsonProperty("pet_size") PetSize petSize,
        @JsonProperty("wheelchair_accessible") Boolean wheelchairAccessible,
        @JsonProperty("indoor_pet") Boolean indoorPet,
        @JsonProperty("max_price") @Min(0) Integer maxPrice,
        @Size(max = 20) List<@Size(max = 100) String> preferences
) {
    public CourseRecommendationRequest {
        preferences = preferences == null ? List.of() : List.copyOf(preferences);
        if (petSize != null && !Boolean.TRUE.equals(petAllowed)) {
            throw new IllegalArgumentException("pet_size requires pet_allowed=true");
        }
    }

    public enum PetSize { SMALL, MEDIUM, LARGE }
}
