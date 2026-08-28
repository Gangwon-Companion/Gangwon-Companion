package com.gangwon.companion.domain.search.indexer;

public record DebeziumPlaceChange(String domain, long id, boolean rootDeleted, long sourceTimestampMillis) {
    public String placeId() {
        return domain + ":" + id;
    }
}
