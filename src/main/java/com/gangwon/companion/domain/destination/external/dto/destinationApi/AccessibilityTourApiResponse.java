package com.gangwon.companion.domain.destination.external.dto.destinationApi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccessibilityTourApiResponse implements DestinationApiResponse<AccessibilityTourApiItem>{
    private Response response;

    public List<AccessibilityTourApiItem> getItems() {
        if (response == null
                || response.body == null
                || response.body.items == null
                || response.body.items.item == null) {
            return Collections.emptyList();
        }
        return response.body.items.item;
    }

    public int getTotalCount() {
        if (response == null || response.body == null || response.body.totalCount == null) {
            return 0;
        }
        return response.body.totalCount;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Response {
        private AccessibilityTourApiResponse.Body body;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {
        private AccessibilityTourApiResponse.Items items;
        private Integer totalCount;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Items {
        private List<AccessibilityTourApiItem> item;
    }
}
