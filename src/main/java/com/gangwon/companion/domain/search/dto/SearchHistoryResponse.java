package com.gangwon.companion.domain.search.dto;

import java.time.LocalDateTime;

public record SearchHistoryResponse(
        Long id,
        String keyword,
        String region,
        LocalDateTime searchedAt
) {
}
