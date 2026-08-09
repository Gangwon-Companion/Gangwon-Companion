package com.gangwon.companion.domain.destination.external.dto.detailApi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DetailApiResponse<T> {


    public List<T> getItems() {
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

    private Response<T> response;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Response<T> {
        private Body<T> body;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body<T> {
        private Items<T> items;
        private Integer totalCount;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Items<T>{
        private List<T> item;
    }

}
