package com.gangwon.companion.domain.activity.client;

import java.time.LocalDateTime;

public record TourApiActivityItem(
        Long contentId,
        int contentTypeId,
        String categoryCode2,
        String title,
        String address,
        String imageUrl,
        String thumbnailUrl,
        String telephone,
        Double latitude,
        Double longitude,
        LocalDateTime modifiedAt
) {
}
