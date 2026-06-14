package com.gangwon.companion.global.web;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long reviewId,
        String nickname,
        String content,
        Double rating,
        LocalDateTime createdAt
) {
}
