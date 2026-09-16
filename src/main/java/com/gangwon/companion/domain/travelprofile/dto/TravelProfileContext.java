package com.gangwon.companion.domain.travelprofile.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gangwon.companion.domain.travelprofile.entity.TravelProfile;

import java.util.List;

public record TravelProfileContext(
        @JsonProperty("traveler_type") String travelerType,
        List<String> tags,
        Double confidence,
        @JsonProperty("analysis_version") String analysisVersion
) {
    public static TravelProfileContext from(TravelProfile profile) {
        return new TravelProfileContext(
                profile.getTravelerType().name(), List.copyOf(profile.getTags()),
                profile.getConfidence(), profile.getAnalysisVersion()
        );
    }
}
