package com.gangwon.companion.domain.search.service;

import com.gangwon.companion.domain.search.dto.PlaceSearchRequest;
import com.gangwon.companion.domain.search.dto.PlaceSearchResponse;

public interface PlaceSearchEngine {
    PlaceSearchResponse search(PlaceSearchRequest request);
}
