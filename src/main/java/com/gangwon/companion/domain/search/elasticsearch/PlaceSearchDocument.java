package com.gangwon.companion.domain.search.elasticsearch;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PlaceSearchDocument(
        String placeId, String domain, String name, String address, String regionCode,
        String searchText, Location location, Boolean petAllowed, Boolean smallPetAllowed,
        Boolean mediumPetAllowed, Boolean largePetAllowed, Boolean wheelchairAccessible,
        String themeName, String menuType, Double rating, Long price, String petInfoText,
        String accessibilityInfoText, String opensAt, String closesAt, String operatingHoursRaw,
        String updatedAt, Integer documentVersion,
        String source, List<String> evidenceFields
) {
    public PlaceSearchDocument(
            String placeId, String domain, String name, String address, String regionCode,
            String searchText, Location location, Boolean petAllowed, Boolean smallPetAllowed,
            Boolean mediumPetAllowed, Boolean largePetAllowed, Boolean wheelchairAccessible,
            String themeName, String menuType, Double rating, Long price, String petInfoText,
            String accessibilityInfoText, String updatedAt, Integer documentVersion,
            String source, List<String> evidenceFields
    ) {
        this(placeId, domain, name, address, regionCode, searchText, location, petAllowed,
                smallPetAllowed, mediumPetAllowed, largePetAllowed, wheelchairAccessible,
                themeName, menuType, rating, price, petInfoText, accessibilityInfoText,
                null, null, null, updatedAt, documentVersion, source, evidenceFields);
    }

    public record Location(double lat, double lon) {}
}
