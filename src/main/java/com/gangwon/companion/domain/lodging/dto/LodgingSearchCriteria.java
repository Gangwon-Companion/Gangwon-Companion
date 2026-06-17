package com.gangwon.companion.domain.lodging.dto;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public record LodgingSearchCriteria(
        String keyword,
        String region,
        Long minPrice,
        Long maxPrice,
        Double rating,
        int page,
        int size
) {
    public Pageable pageable() {
        return PageRequest.of(page, size);
    }
}
