package com.gangwon.companion.domain.travelprofile.dto;
import com.gangwon.companion.domain.travelprofile.entity.TravelProfile;
import java.time.Instant;
import java.util.List;
public record TravelProfileResponse(String status, String travelerType, String title, String description,
                                    List<String> tags, List<String> evidences, Double confidence, Instant analyzedAt) {
    public static TravelProfileResponse notAnalyzed() { return new TravelProfileResponse("NOT_ANALYZED", null, null, null, List.of(), List.of(), null, null); }
    public static TravelProfileResponse from(TravelProfile p) {
        return new TravelProfileResponse(p.getStatus().name(), p.getTravelerType() == null ? null : p.getTravelerType().name(),
                p.getTitle(), p.getDescription(), List.copyOf(p.getTags()), List.copyOf(p.getEvidences()), p.getConfidence(), p.getAnalyzedAt());
    }
}
