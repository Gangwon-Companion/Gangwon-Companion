package com.gangwon.companion.domain.course.dto;

import com.gangwon.companion.domain.course.entity.CourseRecommendationJob;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record CourseRecommendationJobResponse(
        UUID jobId,
        CourseRecommendationJob.Status status,
        JsonNode result,
        String errorCode,
        String message,
        Instant createdAt,
        Instant updatedAt
) {
}
