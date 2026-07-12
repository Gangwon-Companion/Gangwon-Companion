package com.gangwon.companion.domain.activity.dto;

import java.util.List;

public record ActivityListResponse(
        List<ActivitySummaryResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
