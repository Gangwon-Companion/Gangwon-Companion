package com.gangwon.companion.domain.restaurant.dto.response;

import com.gangwon.companion.global.web.AbstractListResponse;

import java.util.List;

public class RestaurantListResponse extends AbstractListResponse<RestaurantItemResponse> {

    public RestaurantListResponse(long totalCount, List<RestaurantItemResponse> items) {
        super(totalCount, items);
    }
}
