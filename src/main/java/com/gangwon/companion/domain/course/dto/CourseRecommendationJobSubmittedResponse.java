package com.gangwon.companion.domain.course.dto;

import com.gangwon.companion.domain.course.entity.CourseRecommendationJob;

import java.util.UUID;

public record CourseRecommendationJobSubmittedResponse(
        UUID jobId,
        CourseRecommendationJob.Status status
) {
}
