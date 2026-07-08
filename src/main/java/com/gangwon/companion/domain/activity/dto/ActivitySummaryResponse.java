package com.gangwon.companion.domain.activity.dto;

import com.gangwon.companion.domain.activity.entity.ActivityCategory;

public record ActivitySummaryResponse(
        Long id,
        Long tourContentId,
        String title,
        ActivityCategory category,
        String region,
        String address,
        String imageUrl,
        String thumbnailUrl,
        Double latitude,
        Double longitude
) {
}
