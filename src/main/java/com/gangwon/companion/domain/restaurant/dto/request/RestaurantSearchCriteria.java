package com.gangwon.companion.domain.restaurant.dto.request;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public record RestaurantSearchCriteria(
        String keyword,
        String menuType,
        String region,
        int page,
        int size
) {
    public Pageable pageable() {
        return PageRequest.of(page, size);
    }
}