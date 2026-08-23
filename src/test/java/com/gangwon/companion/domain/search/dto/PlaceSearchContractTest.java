package com.gangwon.companion.domain.search.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceSearchContractTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesSharedRequestFixture() throws Exception {
        PlaceSearchRequest request = objectMapper.readValue(
                fixture("search_request.json"), PlaceSearchRequest.class);

        assertThat(request.domain()).isEqualTo(PlaceSearchRequest.Domain.DESTINATION);
        assertThat(request.regionCodes()).containsExactly(PlaceSearchRequest.RegionCode.GANGNEUNG);
        assertThat(request.hardFilters().petAllowed()).isTrue();
        assertThat(request.hardFilters().petSize()).isEqualTo(PlaceSearchRequest.PetSize.SMALL);
        assertThat(request.hardFilters().wheelchairAccessible()).isNull();
    }

    @Test
    void deserializesSharedResponseFixtureAndKeepsEvidenceState() throws Exception {
        PlaceSearchResponse response = objectMapper.readValue(
                fixture("search_response.json"), PlaceSearchResponse.class);

        assertThat(response.results()).hasSize(2);
        assertThat(response.results().get(0).status()).isEqualTo(PlaceSearchResponse.Status.OK);
        assertThat(response.results().get(0).evidence().get(0).source()).isEqualTo("TOUR_API");
        assertThat(response.results().get(1).status())
                .isEqualTo(PlaceSearchResponse.Status.INSUFFICIENT_EVIDENCE);
        assertThat(response.results().get(1).missingFields())
                .containsExactly("wheelchair_accessible");
    }

    @Test
    void keepsFalseDistinctFromMissingFilter() throws Exception {
        String json = """
                {
                  "domain": "RESTAURANT",
                  "slot": "D1_LUNCH",
                  "region_codes": ["GANGNEUNG"],
                  "query_text": "",
                  "hard_filters": {
                    "pet_allowed": false,
                    "pet_size": null,
                    "wheelchair_accessible": false
                  },
                  "soft_preferences": {},
                  "geo": null,
                  "limit": 5
                }
                """;

        PlaceSearchRequest request = objectMapper.readValue(json, PlaceSearchRequest.class);

        assertThat(request.hardFilters().petAllowed()).isFalse();
        assertThat(request.hardFilters().petSize()).isNull();
        assertThat(request.hardFilters().wheelchairAccessible()).isFalse();
    }

    private InputStream fixture(String name) {
        return PlaceSearchContractTest.class.getResourceAsStream("/search-contract/" + name);
    }
}
