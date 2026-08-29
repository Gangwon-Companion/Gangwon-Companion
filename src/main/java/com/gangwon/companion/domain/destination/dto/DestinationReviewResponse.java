package com.gangwon.companion.domain.destination.dto;

import java.time.LocalDateTime;

public record DestinationReviewResponse(
        Long reviewId,
        String nickname,
        String content,
        Double rating,
        LocalDateTime createdAt
) {
}
