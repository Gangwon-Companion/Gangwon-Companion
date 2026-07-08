package com.gangwon.companion.domain.home.dto;

import java.util.List;

public record PromotionBannerListResponse(
        boolean festivalAvailable,
        String message,
        List<PromotionBannerResponse> items
) {
}
