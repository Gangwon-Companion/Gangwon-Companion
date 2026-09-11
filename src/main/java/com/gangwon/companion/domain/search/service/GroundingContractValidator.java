package com.gangwon.companion.domain.search.service;

import com.gangwon.companion.domain.search.dto.PlaceSearchResponse;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Validates the evidence contract consumed by the AI search and answer agents.
 * A malformed evidence response must never become LLM context.
 */
@Component
public class GroundingContractValidator {
    public static final String CONTRACT_VERSION = "v1";

    public void validate(PlaceSearchResponse response) {
        if (response == null || response.results() == null) {
            throw new IllegalStateException("Grounding response must contain results");
        }

        for (PlaceSearchResponse.Candidate candidate : response.results()) {
            if (candidate == null || candidate.placeId() == null || candidate.placeId().isBlank()) {
                throw new IllegalStateException("Grounding candidate must contain place_id");
            }
            if (candidate.status() == PlaceSearchResponse.Status.OK && !candidate.missingFields().isEmpty()) {
                throw new IllegalStateException("OK candidate cannot contain missing_fields: " + candidate.placeId());
            }
            if (candidate.status() == PlaceSearchResponse.Status.INSUFFICIENT_EVIDENCE
                    && candidate.missingFields().isEmpty()) {
                throw new IllegalStateException(
                        "INSUFFICIENT_EVIDENCE candidate must contain missing_fields: " + candidate.placeId());
            }

            Set<String> missingFields = new HashSet<>(candidate.missingFields());
            for (PlaceSearchResponse.Evidence evidence : candidate.evidence()) {
                if (evidence == null || evidence.field() == null || evidence.field().isBlank()
                        || evidence.source() == null || evidence.source().isBlank()) {
                    throw new IllegalStateException("Grounding evidence must contain field and source: " + candidate.placeId());
                }
                if (missingFields.contains(evidence.field())) {
                    throw new IllegalStateException(
                            "A field cannot be both evidence and missing: " + evidence.field());
                }
            }
        }
    }
}
