package com.gangwon.companion.domain.search.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gangwon.companion.domain.search.dto.PlaceSearchRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ElasticsearchPlaceSearchEngineTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final ElasticsearchHttpClient client = mock(ElasticsearchHttpClient.class);
    private final ElasticsearchProperties properties = new ElasticsearchProperties();
    private final ElasticsearchPlaceSearchEngine engine = new ElasticsearchPlaceSearchEngine(client, properties, mapper);

    @Test
    void buildsCommonFiltersAndMapsInsufficientEvidence() throws Exception {
        when(client.post(eq("/gangwon-places/_search"), org.mockito.ArgumentMatchers.any())).thenReturn(mapper.readTree("""
                {"hits":{"hits":[{"_score":1.5,"_source":{
                  "placeId":"RESTAURANT:1","domain":"RESTAURANT","name":"강릉 카페",
                  "address":"강원특별자치도 강릉시","regionCode":"GANGNEUNG",
                  "searchText":"강릉 카페 바다","location":{"lat":37.75,"lon":128.90},
                  "source":"TOUR_API","evidenceFields":[]
                }}]}}
                """));
        PlaceSearchRequest request = new PlaceSearchRequest(PlaceSearchRequest.Domain.RESTAURANT, "D1_LUNCH",
                List.of(PlaceSearchRequest.RegionCode.GANGNEUNG), "카페",
                new PlaceSearchRequest.HardFilters(true, PlaceSearchRequest.PetSize.SMALL, true),
                Map.of("ocean_view", 0.8), null, 5);

        var response = engine.search(request);

        assertThat(response.results()).hasSize(1);
        assertThat(response.results().get(0).missingFields())
                .containsExactly("pet_allowed", "pet_size", "wheelchair_accessible");
        assertThat(response.results().get(0).matchedPreferences()).containsExactly("ocean_view");
        ArgumentCaptor<Object> body = ArgumentCaptor.forClass(Object.class);
        verify(client).post(eq("/gangwon-places/_search"), body.capture());
        String json = mapper.writeValueAsString(body.getValue());
        assertThat(json).contains("GANGNEUNG", "petAllowed", "smallPetAllowed", "wheelchairAccessible",
                "multi_match", "must_not", "function_score", "field_value_factor", "themeName^3");
        assertThat(json).doesNotContain("\"petAllowed\":true", "\"smallPetAllowed\":true");
    }

    @Test
    void includesGeoFilterAndReturnsDistance() throws Exception {
        when(client.post(eq("/gangwon-places/_search"), org.mockito.ArgumentMatchers.any())).thenReturn(mapper.readTree("""
                {"hits":{"hits":[{"_score":0.0,"_source":{
                  "placeId":"LODGING:1","domain":"LODGING","name":"숙소","address":"강릉",
                  "regionCode":"GANGNEUNG","searchText":"숙소","location":{"lat":37.76,"lon":128.90},
                  "source":"TOUR_API","evidenceFields":[]
                }}]}}
                """));
        var geo = new PlaceSearchRequest.GeoConstraint(new PlaceSearchRequest.GeoCenter(37.75, 128.90), 5.0);
        var request = new PlaceSearchRequest(PlaceSearchRequest.Domain.LODGING, "D1_LODGING", List.of(), "",
                new PlaceSearchRequest.HardFilters(null, null, null), Map.of(), geo, 5);

        var candidate = engine.search(request).results().get(0);

        assertThat(candidate.distanceKm()).isBetween(1.0, 1.2);
    }
}
