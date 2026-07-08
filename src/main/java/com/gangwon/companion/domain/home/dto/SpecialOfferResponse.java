package com.gangwon.companion.domain.home.dto;

public record SpecialOfferResponse(
        Long id,
        String title,
        String region,
        String category,
        int originalPrice,
        int salePrice,
        int discountRate,
        String reason,
        String imageUrl,
        String linkUrl
) {
}
