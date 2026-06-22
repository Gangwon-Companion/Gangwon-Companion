package com.gangwon.companion.domain.lodging.dto.response;

import java.time.LocalDateTime;

public record LodgingReviewResponse(
        Long reviewId,
        String nickname,
        String content,
        Double rating,
        LocalDateTime createdAt
) {
}