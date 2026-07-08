package com.gangwon.companion.domain.home.dto;

import java.time.LocalDate;

public record PromotionBannerResponse(
        Long id,
        String category,
        String title,
        String description,
        String region,
        String imageUrl,
        LocalDate startDate,
        LocalDate endDate,
        String linkUrl
) {
}
