package com.gangwon.companion.domain.restaurant.dto;

import com.gangwon.companion.domain.restaurant.entity.Restaurant;
import lombok.Getter;

@Getter
public class RestaurantItemResponse {

    private final Long restaurantId;
    private final String name;
    private final String thumbnailUrl;
    private final String menuType;
    private final String region;
    private final Double rating;

    public RestaurantItemResponse(Restaurant restaurant) {
        this.restaurantId = restaurant.getId();
        this.name = restaurant.getName();
        this.thumbnailUrl = restaurant.getThumbnailUrl();
        this.menuType = restaurant.getMenuType();
        this.region = restaurant.getRegion();
        this.rating = restaurant.getRating();
    }
}
