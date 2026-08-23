package com.gangwon.companion.domain.search.elasticsearch;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PlaceSearchDocument(
        String placeId, String domain, String name, String address, String regionCode,
        String searchText, Location location, Boolean petAllowed, Boolean smallPetAllowed,
        Boolean mediumPetAllowed, Boolean largePetAllowed, Boolean wheelchairAccessible,
        String source, List<String> evidenceFields
) {
    public record Location(double lat, double lon) {}
}
