package com.gangwon.companion.domain.user.dto.response;

import java.time.LocalDateTime;

public record MyReviewResponse(
        String placeType,
        Long placeId,
        String placeName,
        Long reviewId,
        String content,
        Double rating,
        LocalDateTime createdAt
) {
}
