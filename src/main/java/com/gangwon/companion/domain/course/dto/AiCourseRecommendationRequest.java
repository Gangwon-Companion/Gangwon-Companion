package com.gangwon.companion.domain.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gangwon.companion.domain.travelprofile.dto.TravelProfileContext;

import java.util.List;

/** Internal BE-to-AI contract. The profile is loaded by BE and cannot be supplied by FE. */
public record AiCourseRecommendationRequest(
        String message,
        String region,
        @JsonProperty("travel_days") Integer travelDays,
        Integer nights,
        @JsonProperty("pet_allowed") Boolean petAllowed,
        @JsonProperty("pet_size") CourseRecommendationRequest.PetSize petSize,
        @JsonProperty("wheelchair_accessible") Boolean wheelchairAccessible,
        @JsonProperty("indoor_pet") Boolean indoorPet,
        @JsonProperty("max_price") Integer maxPrice,
        List<String> preferences,
        @JsonProperty("travel_profile") TravelProfileContext travelProfile
) {
    public static AiCourseRecommendationRequest from(
            CourseRecommendationRequest request,
            TravelProfileContext profile
    ) {
        return new AiCourseRecommendationRequest(
                request.message(), request.region(), request.travelDays(), request.nights(),
                request.petAllowed(), request.petSize(), request.wheelchairAccessible(),
                request.indoorPet(), request.maxPrice(), request.preferences(), profile
        );
    }
}
