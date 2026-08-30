package com.gangwon.companion.domain.destination.dto;

import java.time.LocalDateTime;

public record DestinationReviewResponse(
        Long reviewId,
        String nickname,
        String profileImageUrl,
        String content,
        Double rating,
        LocalDateTime createdAt,
        boolean isMine
) {
}
