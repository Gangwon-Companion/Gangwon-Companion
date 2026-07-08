package com.gangwon.companion.domain.activity.dto;

import com.gangwon.companion.domain.activity.entity.ActivityCategory;

public record ActivityDetailResponse(
        Long id,
        Long tourContentId,
        String title,
        ActivityCategory category,
        String region,
        String address,
        String imageUrl,
        String telephone,
        String homepageUrl,
        String overview,
        String operatingHours,
        String restDate,
        String usageFee,
        String parkingInfo,
        String reservationUrl,
        Double latitude,
        Double longitude
) {
}
