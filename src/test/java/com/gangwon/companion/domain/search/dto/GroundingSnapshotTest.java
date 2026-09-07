package com.gangwon.companion.domain.search.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gangwon.companion.domain.search.service.SearchTraceRecorder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GroundingSnapshotTest {
    private PlaceSearchResponse.Candidate candidate(String name, double score,
                                                     List<PlaceSearchResponse.Evidence> evidence) {
        return new PlaceSearchResponse.Candidate("DESTINATION:1", PlaceSearchRequest.Domain.DESTINATION,
                name, null, null, 1.2, score, PlaceSearchResponse.Status.INSUFFICIENT_EVIDENCE,
                List.of("operating_hours"), List.of("quiet"), evidence);
    }

    @Test
    void versionsFactsIndependentlyOfRankingAndRetrievalTime() {
        var a = SearchTraceRecorder.capture(List.of(candidate("해변", 1, List.of())), "rdb", "keyword");
        var b = SearchTraceRecorder.capture(List.of(candidate("해변", 8, List.of())), "rdb", "relaxed_keyword");
        assertThat(a.trace().requestId()).isNotEqualTo(b.trace().requestId());
        assertThat(a.trace().snapshots().get(0).contentVersion())
                .isEqualTo(b.trace().snapshots().get(0).contentVersion());
        assertThat(a.trace().snapshots().get(0).facts()).containsOnlyKeys("domain", "name");
        assertThat(a.trace().snapshots().get(0).missingFields()).containsExactly("operating_hours");
    }

    @Test
    void detectsPolicyReversalAndDoesNotInferUnknownPolicy() {
        var allowed = GroundingSnapshot.from(candidate("해변", 1,
                List.of(new PlaceSearchResponse.Evidence("pet_allowed", true, "TOUR_API"))), "POSTGRESQL");
        var denied = GroundingSnapshot.from(candidate("해변", 1,
                List.of(new PlaceSearchResponse.Evidence("pet_allowed", false, "TOUR_API"))), "POSTGRESQL");
        var unknown = GroundingSnapshot.from(candidate("해변", 1, List.of()), "POSTGRESQL");
        assertThat(allowed.contentVersion()).isNotEqualTo(denied.contentVersion()).isNotEqualTo(unknown.contentVersion());
        assertThat(unknown.evidence()).isEmpty();
    }

    @Test
    void snapshotsAreDetachedAndSurviveJsonRoundTrip() throws Exception {
        var evidence = new ArrayList<PlaceSearchResponse.Evidence>();
        evidence.add(new PlaceSearchResponse.Evidence("pet_allowed", false, "TOUR_API"));
        var response = SearchTraceRecorder.capture(List.of(candidate("해변", 1, evidence)), "elasticsearch", "filter_only");
        evidence.clear();
        var mapper = new ObjectMapper();
        var restored = mapper.readValue(mapper.writeValueAsBytes(response), PlaceSearchResponse.class);
        assertThat(restored.trace()).isEqualTo(response.trace());
        assertThat(restored.trace().snapshots().get(0).evidence()).hasSize(1);
        assertThat(restored.trace().engine()).isEqualTo("elasticsearch");
        assertThat(restored.trace().path()).isEqualTo("filter_only");
    }
}
