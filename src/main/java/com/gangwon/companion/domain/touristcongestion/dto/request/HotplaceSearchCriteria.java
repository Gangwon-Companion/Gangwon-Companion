package com.gangwon.companion.domain.touristcongestion.dto.request;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public record HotplaceSearchCriteria(
        String keyword,
        String region,
        String period,
        int page,
        int size
) {
    public Pageable pageable() {
        return PageRequest.of(page, size);
    }
}
