package com.gangwon.companion.domain.destination.external.dto.destinationApi;

import java.util.List;

public interface DestinationApiResponse<T extends DestinationApiItem> {

    List<T> getItems();

    int getTotalCount();
}
