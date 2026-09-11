package com.gangwon.companion.domain.search.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gangwon.companion.domain.search.dto.PlaceSearchRequest;
import com.gangwon.companion.domain.search.dto.PlaceSearchResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class ElasticsearchPlaceSearchEngineTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final ElasticsearchHttpClient client = mock(ElasticsearchHttpClient.class);
    private final ElasticsearchProperties properties = new ElasticsearchProperties();
    private final ElasticsearchPlaceSearchEngine engine = new ElasticsearchPlaceSearchEngine(client, properties, mapper);

    @Test
    void buildsCommonFiltersAndMapsInsufficientEvidence() throws Exception {
        stubDiagnosticCounts(1);
        when(client.post(eq("/gangwon-places/_search"), org.mockito.ArgumentMatchers.any())).thenReturn(mapper.readTree("""
                {"hits":{"hits":[{"_score":1.5,"_source":{
                  "placeId":"RESTAURANT:1","domain":"RESTAURANT","name":"강릉 카페",
                  "address":"강원특별자치도 강릉시","regionCode":"GANGNEUNG",
                  "placeSubtype":"CAFE",
                  "searchText":"강릉 카페 바다","location":{"lat":37.75,"lon":128.90},
                  "opensAt":"09:00","closesAt":"22:00","operatingHoursRaw":"09:00~22:00",
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
                .containsExactly("pet_allowed", "wheelchair_accessible");
        assertThat(response.results().get(0).matchedPreferences()).containsExactly("ocean_view");
        assertThat(response.results().get(0).matchedKeywords()).contains("카페", "바다");
        assertThat(response.results().get(0).placeSubtype()).isEqualTo("CAFE");
        assertThat(response.results().get(0).regionCode()).isEqualTo("GANGNEUNG");
        assertThat(response.results().get(0).regionMatch()).isTrue();
        ArgumentCaptor<Object> body = ArgumentCaptor.forClass(Object.class);
        verify(client).post(eq("/gangwon-places/_search"), body.capture());
        String json = mapper.writeValueAsString(body.getValue());
        assertThat(json).contains("GANGNEUNG", "petAllowed", "smallPetAllowed", "wheelchairAccessible",
                "multi_match", "term", "must_not", "function_score", "field_value_factor", "themeName^3",
                "opensAt", "closesAt");
        assertThat(json).contains("\"petAllowed\":true", "\"wheelchairAccessible\":true");
        assertThat(json).doesNotContain("\"smallPetAllowed\":true");
    }

    @Test
    void includesGeoFilterAndReturnsDistance() throws Exception {
        stubDiagnosticCounts(1);
        when(client.post(eq("/gangwon-places/_search"), org.mockito.ArgumentMatchers.any())).thenReturn(mapper.readTree("""
                {"hits":{"hits":[{"_score":0.0,"_source":{
                  "placeId":"LODGING:1","domain":"LODGING","name":"숙소","address":"강릉",
                  "regionCode":"GANGNEUNG","searchText":"숙소","location":{"lat":37.76,"lon":128.90},
                  "opensAt":"15:00","closesAt":"11:00","operatingHoursRaw":"15:00~11:00",
                  "source":"TOUR_API","evidenceFields":[]
                }}]}}
                """));
        var geo = new PlaceSearchRequest.GeoConstraint(new PlaceSearchRequest.GeoCenter(37.75, 128.90), 5.0);
        var request = new PlaceSearchRequest(PlaceSearchRequest.Domain.LODGING, "D1_LODGING", List.of(), "",
                new PlaceSearchRequest.HardFilters(null, null, null), Map.of(), geo, 5);

        var candidate = engine.search(request).results().get(0);

        assertThat(candidate.distanceKm()).isBetween(1.0, 1.2);
    }

    @Test
    void fallsBackToHardFiltersWhenTextMatchesNothing() throws Exception {
        stubDiagnosticCounts(1);
        var empty = mapper.readTree("{\"hits\":{\"hits\":[]}}");
        var fallback = mapper.readTree("""
                {"hits":{"hits":[{"_score":0.2,"_source":{
                  "placeId":"RESTAURANT:64","domain":"RESTAURANT","name":"롱블랙",
                  "address":"강릉","regionCode":"GANGNEUNG","searchText":"카페 커피",
                  "opensAt":"10:00","closesAt":"21:00","source":"TOUR_API","evidenceFields":[]
                }}]}}
                """);
        when(client.post(eq("/gangwon-places/_search"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(empty, empty, fallback);
        var request = new PlaceSearchRequest(PlaceSearchRequest.Domain.RESTAURANT, "D1_LUNCH",
                List.of(PlaceSearchRequest.RegionCode.GANGNEUNG), "강릉 점심 맛집",
                new PlaceSearchRequest.HardFilters(null, null, null), Map.of(), null, 5);

        var response = engine.search(request);

        assertThat(response.results()).extracting(PlaceSearchResponse.Candidate::placeId)
                .containsExactly("RESTAURANT:64");
        ArgumentCaptor<Object> bodies = ArgumentCaptor.forClass(Object.class);
        verify(client, times(3)).post(eq("/gangwon-places/_search"), bodies.capture());
        String fallbackJson = mapper.writeValueAsString(bodies.getAllValues().get(2));
        assertThat(fallbackJson).contains("RESTAURANT", "GANGNEUNG", "opensAt", "closesAt");
        assertThat(fallbackJson).doesNotContain("multi_match", "강릉 점심 맛집");
    }

    @Test
    void boostsAndMatchesExpandedSoftPreferences() throws Exception {
        stubDiagnosticCounts(1);
        when(client.post(eq("/gangwon-places/_search"), org.mockito.ArgumentMatchers.any())).thenReturn(mapper.readTree("""
                {"hits":{"hits":[{"_score":2.0,"_source":{
                  "placeId":"RESTAURANT:7","domain":"RESTAURANT","name":"속초 바다식당",
                  "address":"강원특별자치도 속초시","regionCode":"SOKCHO",
                  "placeSubtype":"RESTAURANT",
                  "searchText":"속초 바다식당 한식 물회 대게 해산물 항구 전망",
                  "opensAt":"09:00","closesAt":"20:00","source":"TOUR_API","evidenceFields":[]
                }}]}}
                """));
        var request = new PlaceSearchRequest(PlaceSearchRequest.Domain.RESTAURANT, "D1_LUNCH",
                List.of(PlaceSearchRequest.RegionCode.SOKCHO), "",
                new PlaceSearchRequest.HardFilters(null, null, null),
                Map.of("food", 0.9, "oceanView", 0.8, "nature", 0.4, "nightView", 0.3), null, 5);

        var response = engine.search(request);

        assertThat(response.results().get(0).matchedPreferences()).containsExactly("food", "oceanView");
        assertThat(response.results().get(0).matchedKeywords())
                .contains("물회", "대게", "해산물", "바다", "항구");
        assertThat(response.results().get(0).matchedPreferenceDetails())
                .extracting(PlaceSearchResponse.MatchedPreference::preference)
                .containsExactly("food", "oceanView");
        assertThat(response.results().get(0).matchedPreferenceDetails().get(0).fields())
                .contains("searchText");
        assertThat(response.results().get(0).placeSubtype()).isEqualTo("RESTAURANT");
        ArgumentCaptor<Object> body = ArgumentCaptor.forClass(Object.class);
        verify(client).post(eq("/gangwon-places/_search"), body.capture());
        String json = mapper.writeValueAsString(body.getValue());
        assertThat(json).contains("물회", "대게", "해산물", "항구", "계곡", "야경");
        assertThat(json).doesNotContain("multi_match");
    }

    @Test
    void allowsShortHoeKeywordOnlyInFoodContext() throws Exception {
        stubDiagnosticCounts(1);
        when(client.post(eq("/gangwon-places/_search"), org.mockito.ArgumentMatchers.any())).thenReturn(mapper.readTree("""
                {"hits":{"hits":[{"_score":2.0,"_source":{
                  "placeId":"RESTAURANT:11","domain":"RESTAURANT","name":"팔팔회센타",
                  "address":"강원특별자치도 속초시","regionCode":"SOKCHO",
                  "placeSubtype":"RESTAURANT",
                  "searchText":"속초 팔팔회센타 물회 대게",
                  "opensAt":"09:00","closesAt":"20:00","source":"TOUR_API","evidenceFields":[]
                }}]}}
                """));
        var request = new PlaceSearchRequest(PlaceSearchRequest.Domain.RESTAURANT, "D1_LUNCH",
                List.of(PlaceSearchRequest.RegionCode.SOKCHO), "회",
                new PlaceSearchRequest.HardFilters(null, null, null),
                Map.of("food", 0.9), null, 5);

        var candidate = engine.search(request).results().get(0);

        assertThat(candidate.matchedPreferences()).containsExactly("food");
        assertThat(candidate.matchedKeywords()).contains("회", "물회", "대게");
    }

    @Test
    void rejectsShortMountainKeywordWhenItOnlyAppearsInsideUnrelatedWordsOrRoadNames() throws Exception {
        stubDiagnosticCounts(1);
        when(client.post(eq("/gangwon-places/_search"), org.mockito.ArgumentMatchers.any())).thenReturn(mapper.readTree("""
                {"hits":{"hits":[{"_score":2.0,"_source":{
                  "placeId":"RESTAURANT:123","domain":"RESTAURANT","name":"카페 설악산로",
                  "address":"강원특별자치도 속초시 설악산 로",
                  "regionCode":"SOKCHO","placeSubtype":"CAFE",
                  "searchText":"카페 설악산 로 설악산코코 해산물 메뉴",
                  "opensAt":"09:00","closesAt":"20:00","source":"TOUR_API","evidenceFields":[]
                }}]}}
                """));
        var request = new PlaceSearchRequest(PlaceSearchRequest.Domain.RESTAURANT, "D1_CAFE",
                List.of(PlaceSearchRequest.RegionCode.SOKCHO), "산",
                new PlaceSearchRequest.HardFilters(null, null, null),
                Map.of("nature", 0.9), null, 5);

        var response = engine.search(request);

        assertThat(response.results().get(0).matchedPreferences()).isEmpty();
        assertThat(response.results().get(0).matchedKeywords()).isEmpty();
        assertThat(response.diagnostics().underMatchedPreferences()).containsExactly("nature");
        assertThat(response.diagnostics().unmatchedQueryTerms()).containsExactly("산");
    }

    @Test
    void allowsShortMountainKeywordInMountainContext() throws Exception {
        stubDiagnosticCounts(1);
        when(client.post(eq("/gangwon-places/_search"), org.mockito.ArgumentMatchers.any())).thenReturn(mapper.readTree("""
                {"hits":{"hits":[{"_score":2.0,"_source":{
                  "placeId":"DESTINATION:123","domain":"DESTINATION","name":"설악산 국립공원",
                  "address":"강원특별자치도 속초시","regionCode":"SOKCHO",
                  "placeSubtype":"DESTINATION",
                  "searchText":"설악산 국립공원 등산 산 정상 전망",
                  "opensAt":"09:00","closesAt":"20:00","source":"TOUR_API","evidenceFields":[]
                }}]}}
                """));
        var request = new PlaceSearchRequest(PlaceSearchRequest.Domain.DESTINATION, "D1_DESTINATION",
                List.of(PlaceSearchRequest.RegionCode.SOKCHO), "산",
                new PlaceSearchRequest.HardFilters(null, null, null),
                Map.of("nature", 0.9), null, 5);

        var candidate = engine.search(request).results().get(0);

        assertThat(candidate.matchedPreferences()).containsExactly("nature");
        assertThat(candidate.matchedKeywords()).contains("산", "등산");
    }

    @Test
    void boostsAndMatchesPetAndAccessibilityPreferencesAgainstDedicatedTextFields() throws Exception {
        stubDiagnosticCounts(1);
        when(client.post(eq("/gangwon-places/_search"), org.mockito.ArgumentMatchers.any())).thenReturn(mapper.readTree("""
                {"hits":{"hits":[{"_score":2.0,"_source":{
                  "placeId":"DESTINATION:7","domain":"DESTINATION","name":"속초 산책로",
                  "address":"강원특별자치도 속초시","regionCode":"SOKCHO",
                  "searchText":"속초 산책로",
                  "petInfoText":"일부구역 동반가능 소형견 목줄 착용",
                  "accessibilityInfoText":"출입구까지 턱이 없어 휠체어 접근 가능함 장애인 화장실 있음",
                  "opensAt":"09:00","closesAt":"18:00","source":"TOUR_API","evidenceFields":[]
                }}]}}
                """));
        var request = new PlaceSearchRequest(PlaceSearchRequest.Domain.DESTINATION, "D1_DESTINATION",
                List.of(PlaceSearchRequest.RegionCode.SOKCHO), "",
                new PlaceSearchRequest.HardFilters(null, null, null),
                Map.of("smallPet", 0.8, "wheelchair", 0.9, "accessibleRestroom", 0.7), null, 5);

        var response = engine.search(request);

        assertThat(response.results().get(0).matchedPreferences())
                .containsExactly("accessibleRestroom", "smallPet", "wheelchair");
        ArgumentCaptor<Object> body = ArgumentCaptor.forClass(Object.class);
        verify(client).post(eq("/gangwon-places/_search"), body.capture());
        String json = mapper.writeValueAsString(body.getValue());
        assertThat(json).contains("petInfoText", "accessibilityInfoText", "소형견", "휠체어", "장애인 화장실");
    }

    @Test
    void includesEstimatedOperatingHoursEvidenceWhenParsedHoursAreFallback() throws Exception {
        stubDiagnosticCounts(1);
        when(client.post(eq("/gangwon-places/_search"), org.mockito.ArgumentMatchers.any())).thenReturn(mapper.readTree("""
                {"hits":{"hits":[{"_score":1.0,"_source":{
                  "placeId":"DESTINATION:9","domain":"DESTINATION","name":"상시개방 산책로",
                  "address":"강원특별자치도 속초시","regionCode":"SOKCHO",
                  "searchText":"상시개방 산책로",
                  "opensAt":"00:00","closesAt":"22:00","operatingHoursRaw":"상시 개방",
                  "source":"TOUR_API","evidenceFields":["opens_at","closes_at","operating_hours_estimated"]
                }}]}}
                """));
        var request = new PlaceSearchRequest(PlaceSearchRequest.Domain.DESTINATION, "D1_DESTINATION",
                List.of(PlaceSearchRequest.RegionCode.SOKCHO), "",
                new PlaceSearchRequest.HardFilters(null, null, null), Map.of(), null, 5);

        var candidate = engine.search(request).results().get(0);

        assertThat(candidate.status()).isEqualTo(PlaceSearchResponse.Status.OK);
        assertThat(candidate.missingFields()).isEmpty();
        assertThat(candidate.evidence())
                .extracting(PlaceSearchResponse.Evidence::field)
                .contains("opens_at", "closes_at", "operating_hours_estimated", "operating_hours_raw");
    }

    @Test
    void mapsPolicyFieldsAndEvidenceWithAiAgentContractNames() throws Exception {
        stubDiagnosticCounts(1);
        when(client.post(eq("/gangwon-places/_search"), org.mockito.ArgumentMatchers.any())).thenReturn(mapper.readTree("""
                {"hits":{"hits":[{"_score":1.0,"_source":{
                  "placeId":"DESTINATION:10","domain":"DESTINATION","name":"반려견 산책로",
                  "address":"강원특별자치도 속초시","regionCode":"SOKCHO",
                  "searchText":"반려견 산책 휠체어 접근",
                  "petAllowed":true,"smallPetAllowed":true,"mediumPetAllowed":false,"largePetAllowed":false,
                  "wheelchairAccessible":true,
                  "opensAt":"09:00","closesAt":"18:00","source":"TOUR_API","evidenceFields":[]
                }}]}}
                """));
        var request = new PlaceSearchRequest(PlaceSearchRequest.Domain.DESTINATION, "D1_DESTINATION",
                List.of(PlaceSearchRequest.RegionCode.SOKCHO), "",
                new PlaceSearchRequest.HardFilters(true, PlaceSearchRequest.PetSize.SMALL, true),
                Map.of(), null, 5);

        var candidate = engine.search(request).results().get(0);

        assertThat(candidate.status()).isEqualTo(PlaceSearchResponse.Status.OK);
        assertThat(candidate.petAllowed()).isTrue();
        assertThat(candidate.maxPetSize()).isEqualTo("SMALL");
        assertThat(candidate.wheelchairAccessible()).isTrue();
        assertThat(candidate.evidence())
                .extracting(PlaceSearchResponse.Evidence::field)
                .contains("pet_allowed", "max_pet_size", "wheelchair_accessible");

        ArgumentCaptor<Object> body = ArgumentCaptor.forClass(Object.class);
        verify(client).post(eq("/gangwon-places/_search"), body.capture());
        String json = mapper.writeValueAsString(body.getValue());
        assertThat(json).contains("\"petAllowed\":true", "\"wheelchairAccessible\":true");
    }

    @Test
    void fallsBackWithInsufficientEvidenceWhenNoStrictPolicyCandidatesExist() throws Exception {
        var empty = mapper.readTree("{\"hits\":{\"hits\":[]}}");
        var fallback = mapper.readTree("""
                {"hits":{"hits":[{"_score":0.3,"_source":{
                  "placeId":"RESTAURANT:77","domain":"RESTAURANT","name":"정책 근거 없는 식당",
                  "address":"강원특별자치도 강릉시","regionCode":"GANGNEUNG",
                  "searchText":"강릉 맛집",
                  "opensAt":"10:00","closesAt":"20:00","source":"TOUR_API","evidenceFields":[]
                }}]}}
                """);
        when(client.post(eq("/gangwon-places/_search"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(empty, fallback);
        when(client.post(eq("/gangwon-places/_count"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(
                        mapper.readTree("{\"count\":0}"),
                        mapper.readTree("{\"count\":0}"),
                        mapper.readTree("{\"count\":8}"),
                        mapper.readTree("{\"count\":0}"),
                        mapper.readTree("{\"count\":8}"),
                        mapper.readTree("{\"count\":20}")
                );
        var request = new PlaceSearchRequest(PlaceSearchRequest.Domain.RESTAURANT, "D1_LUNCH",
                List.of(PlaceSearchRequest.RegionCode.GANGNEUNG), "",
                new PlaceSearchRequest.HardFilters(true, null, true), Map.of(), null, 5);

        var response = engine.search(request);
        var candidate = response.results().get(0);

        assertThat(candidate.status()).isEqualTo(PlaceSearchResponse.Status.INSUFFICIENT_EVIDENCE);
        assertThat(candidate.petAllowed()).isNull();
        assertThat(candidate.wheelchairAccessible()).isNull();
        assertThat(candidate.missingFields()).contains("pet_allowed", "wheelchair_accessible");
        assertThat(response.diagnostics().missingEvidenceFields())
                .contains("pet_allowed", "wheelchair_accessible");
        assertThat(response.diagnostics().failureReasons())
                .contains(PlaceSearchResponse.FailureReason.NO_POLICY_EVIDENCE);
        assertThat(response.diagnostics().suggestedActions())
                .contains(PlaceSearchResponse.SuggestedAction.ASK_USER_TO_ADJUST_REQUIRED_CONDITION);

        ArgumentCaptor<Object> bodies = ArgumentCaptor.forClass(Object.class);
        verify(client, times(2)).post(eq("/gangwon-places/_search"), bodies.capture());
        String strictJson = mapper.writeValueAsString(bodies.getAllValues().get(0));
        String fallbackJson = mapper.writeValueAsString(bodies.getAllValues().get(1));
        assertThat(strictJson).contains("\"petAllowed\":true", "\"wheelchairAccessible\":true");
        assertThat(fallbackJson).doesNotContain("\"petAllowed\":true", "\"wheelchairAccessible\":true");
    }

    @Test
    void returnsDiagnosticsWhenResultsAreShort() throws Exception {
        when(client.post(eq("/gangwon-places/_search"), org.mockito.ArgumentMatchers.any())).thenReturn(mapper.readTree("""
                {"hits":{"hits":[]}}
                """));
        when(client.post(eq("/gangwon-places/_count"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(
                        mapper.readTree("{\"count\":0}"),
                        mapper.readTree("{\"count\":12}"),
                        mapper.readTree("{\"count\":0}"),
                        mapper.readTree("{\"count\":3}"),
                        mapper.readTree("{\"count\":20}"),
                        mapper.readTree("{\"count\":100}")
                );
        var request = new PlaceSearchRequest(PlaceSearchRequest.Domain.DESTINATION, "D1_DESTINATION",
                List.of(PlaceSearchRequest.RegionCode.SOKCHO), "바다",
                new PlaceSearchRequest.HardFilters(true, null, null), Map.of(), null, 10);

        var diagnostics = engine.search(request).diagnostics();

        assertThat(diagnostics).isNotNull();
        assertThat(diagnostics.requestedLimit()).isEqualTo(10);
        assertThat(diagnostics.returnedCount()).isZero();
        assertThat(diagnostics.shortage()).isEqualTo(10);
        assertThat(diagnostics.counts())
                .containsEntry("current", 0L)
                .containsEntry("without_query", 12L)
                .containsEntry("without_operating_hours", 3L);
        assertThat(diagnostics.failureReasons())
                .contains(PlaceSearchResponse.FailureReason.NO_TEXT_MATCH,
                        PlaceSearchResponse.FailureReason.NO_OPERATING_HOURS,
                        PlaceSearchResponse.FailureReason.LOW_RESULT_COUNT);
        assertThat(diagnostics.suggestedActions())
                .contains(PlaceSearchResponse.SuggestedAction.EXPAND_QUERY_TEXT,
                        PlaceSearchResponse.SuggestedAction.DROP_QUERY_TEXT,
                        PlaceSearchResponse.SuggestedAction.USE_OPERATING_HOURS_ESTIMATES,
                        PlaceSearchResponse.SuggestedAction.INCREASE_LIMIT);
        assertThat(diagnostics.suggestedActions())
                .doesNotContain(PlaceSearchResponse.SuggestedAction.ASK_USER_TO_ADJUST_REQUIRED_CONDITION);
        assertThat(diagnostics.unmatchedQueryTerms()).containsExactly("바다");
        assertThat(diagnostics.missingEvidenceFields()).contains("operating_hours", "pet_allowed");
    }

    @Test
    void reportsPolicyEvidenceButDoesNotSuggestAutomaticallyRelaxingRequiredPolicy() throws Exception {
        when(client.post(eq("/gangwon-places/_search"), org.mockito.ArgumentMatchers.any())).thenReturn(mapper.readTree("""
                {"hits":{"hits":[]}}
                """));
        when(client.post(eq("/gangwon-places/_count"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(
                        mapper.readTree("{\"count\":0}"),
                        mapper.readTree("{\"count\":0}"),
                        mapper.readTree("{\"count\":9}"),
                        mapper.readTree("{\"count\":0}"),
                        mapper.readTree("{\"count\":20}"),
                        mapper.readTree("{\"count\":100}")
                );
        var request = new PlaceSearchRequest(PlaceSearchRequest.Domain.DESTINATION, "D1_DESTINATION",
                List.of(PlaceSearchRequest.RegionCode.SOKCHO), "",
                new PlaceSearchRequest.HardFilters(true, PlaceSearchRequest.PetSize.SMALL, true),
                Map.of(), null, 10);

        var diagnostics = engine.search(request).diagnostics();

        assertThat(diagnostics.failureReasons())
                .contains(PlaceSearchResponse.FailureReason.NO_POLICY_EVIDENCE,
                        PlaceSearchResponse.FailureReason.LOW_RESULT_COUNT);
        assertThat(diagnostics.suggestedActions())
                .contains(PlaceSearchResponse.SuggestedAction.ASK_USER_TO_ADJUST_REQUIRED_CONDITION);
        assertThat(diagnostics.suggestedActions())
                .doesNotContain(PlaceSearchResponse.SuggestedAction.EXPAND_QUERY_TEXT,
                        PlaceSearchResponse.SuggestedAction.DROP_QUERY_TEXT);
        assertThat(diagnostics.missingEvidenceFields())
                .contains("operating_hours", "pet_allowed", "wheelchair_accessible");
    }

    private void stubDiagnosticCounts(long count) throws Exception {
        when(client.post(argThat(path -> path != null && path.endsWith("/_count")),
                org.mockito.ArgumentMatchers.any())).thenReturn(mapper.readTree("{\"count\":" + count + "}"));
    }
}
