package com.gangwon.companion.domain.search.service;

import com.gangwon.companion.domain.search.dto.PlaceSearchRequest;
import com.gangwon.companion.domain.search.dto.PlaceSearchResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GroundingContractValidatorTest {
    private final GroundingContractValidator validator = new GroundingContractValidator();

    @Test
    void acceptsGroundedCandidate() {
        var candidate = candidate(
                PlaceSearchResponse.Status.OK,
                List.of(),
                List.of(new PlaceSearchResponse.Evidence("opens_at", "09:00", "TOUR_API")));

        validator.validate(new PlaceSearchResponse(List.of(candidate)));
    }

    @Test
    void rejectsInsufficientEvidenceWithoutMissingFields() {
        var candidate = candidate(PlaceSearchResponse.Status.INSUFFICIENT_EVIDENCE, List.of(), List.of());

        assertThatThrownBy(() -> validator.validate(new PlaceSearchResponse(List.of(candidate))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing_fields");
    }

    @Test
    void rejectsEvidenceThatIsAlsoMissing() {
        var candidate = candidate(
                PlaceSearchResponse.Status.INSUFFICIENT_EVIDENCE,
                List.of("pet_allowed"),
                List.of(new PlaceSearchResponse.Evidence("pet_allowed", true, "TOUR_API")));

        assertThatThrownBy(() -> validator.validate(new PlaceSearchResponse(List.of(candidate))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("both evidence and missing");
    }

    private static PlaceSearchResponse.Candidate candidate(
            PlaceSearchResponse.Status status,
            List<String> missingFields,
            List<PlaceSearchResponse.Evidence> evidence
    ) {
        return new PlaceSearchResponse.Candidate(
                "DESTINATION:1",
                PlaceSearchRequest.Domain.DESTINATION,
                "Test place",
                "Gangwon",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                1.0,
                status,
                missingFields,
                List.of(),
                List.of(),
                List.of(),
                evidence
        );
    }
}
