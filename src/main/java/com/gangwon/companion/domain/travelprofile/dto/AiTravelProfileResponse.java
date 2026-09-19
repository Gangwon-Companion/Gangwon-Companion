package com.gangwon.companion.domain.travelprofile.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
public record AiTravelProfileResponse(Status status,
    @JsonProperty("traveler_type") TravelerType travelerType, String title, String description,
    List<String> tags, List<String> evidences, Double confidence,
    @JsonProperty("axis_scores") TravelProfileAxisScores axisScores,
    @JsonProperty("analysis_version") String analysisVersion) {
    public AiTravelProfileResponse { tags = tags == null ? List.of() : List.copyOf(tags); evidences = evidences == null ? List.of() : List.copyOf(evidences); }
    public enum Status { COMPLETED, INSUFFICIENT_DATA }
    public enum TravelerType { CAPF, CAPH, CASF, CASH, CRPF, CRPH, CRSF, CRSH, NAPF, NAPH, NASF, NASH, NRPF, NRPH, NRSF, NRSH }
}
