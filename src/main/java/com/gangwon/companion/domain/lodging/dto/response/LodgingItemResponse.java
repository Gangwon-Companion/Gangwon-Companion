package com.gangwon.companion.domain.lodging.dto.response;

import com.gangwon.companion.domain.lodging.entity.Lodging;
import lombok.Getter;

@Getter
public class LodgingItemResponse {

    private final Long lodgingId;
    private final String name;
    private final String thumbnailUrl;
    private final String region;
    private final Long price;
    private final Double rating;

    public LodgingItemResponse(Lodging lodging) {
        this.lodgingId = lodging.getId();
        this.name = lodging.getName();
        this.thumbnailUrl = lodging.getThumbnailUrl();
        this.region = lodging.getRegion();
        this.price = lodging.getPrice();
        this.rating = lodging.getRating();
    }
}