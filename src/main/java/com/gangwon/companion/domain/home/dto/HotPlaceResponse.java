package com.gangwon.companion.domain.home.dto;

public record HotPlaceResponse(
        Long id,
        String name,
        String region,
        String description,
        int visitTrendPercent,
        String congestionLevel,
        int interestScore,
        double recommendationScore,
        String imageUrl
) {
}
