package com.gangwon.companion.domain.search.service;

import com.gangwon.companion.domain.search.dto.GroundingSnapshot;
import com.gangwon.companion.domain.search.dto.PlaceSearchResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class SearchTraceRecorder {
    private SearchTraceRecorder() {}

    public static PlaceSearchResponse capture(List<PlaceSearchResponse.Candidate> candidates,
                                               String engine, String path) {
        var results = List.copyOf(candidates);
        var snapshots = results.stream().map(candidate -> GroundingSnapshot.from(candidate,
                "elasticsearch".equals(engine) ? "ELASTICSEARCH_INDEX" : "POSTGRESQL")).toList();
        return new PlaceSearchResponse(results, new PlaceSearchResponse.SearchTrace(
                1, UUID.randomUUID().toString(), Instant.now().toString(), engine, path, snapshots));
    }
}
