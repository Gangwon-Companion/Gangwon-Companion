package com.gangwon.companion.domain.travelprofile.dto;
import com.gangwon.companion.domain.travelprofile.entity.TravelProfileAnalysisJob;
import java.time.Instant;
import java.util.UUID;
public final class TravelProfileJobResponses {
    private TravelProfileJobResponses() {}
    public record Submitted(UUID jobId, TravelProfileAnalysisJob.Status status) {}
    public record Detail(UUID jobId, TravelProfileAnalysisJob.Status status, TravelProfileResponse profile,
                         String errorCode, String message, Instant createdAt, Instant updatedAt) {}
}
