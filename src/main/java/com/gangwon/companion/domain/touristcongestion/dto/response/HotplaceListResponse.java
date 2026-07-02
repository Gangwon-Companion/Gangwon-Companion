package com.gangwon.companion.domain.touristcongestion.dto.response;

import com.gangwon.companion.global.web.AbstractListResponse;

import java.util.List;

public class HotplaceListResponse extends AbstractListResponse<HotplaceItemResponse> {

    public HotplaceListResponse(long totalCount, List<HotplaceItemResponse> items) {
        super(totalCount, items);
    }
}
