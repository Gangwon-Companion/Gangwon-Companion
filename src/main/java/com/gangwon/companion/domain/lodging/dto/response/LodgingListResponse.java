package com.gangwon.companion.domain.lodging.dto.response;

import com.gangwon.companion.global.web.AbstractListResponse;

import java.util.List;

public class LodgingListResponse extends AbstractListResponse<LodgingItemResponse> {

    public LodgingListResponse(long totalCount, List<LodgingItemResponse> items) {
        super(totalCount, items);
    }
}