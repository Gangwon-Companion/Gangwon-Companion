package com.gangwon.companion.domain.restaurant.dto;

import java.time.LocalDateTime;

public record RestaurantReviewResponse(
        Long reviewId,
        String nickname,
        String content,
        Double rating,
        LocalDateTime createdAt
) {
}
