package com.gangwon.companion.domain.search.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record PlaceSearchResponse(List<Candidate> results, Diagnostics diagnostics) {
    public PlaceSearchResponse(List<Candidate> results) {
        this(results, null);
    }

    public enum Status {
        OK,
        INSUFFICIENT_EVIDENCE
    }

    public enum FailureReason {
        NO_TEXT_MATCH,
        NO_REGION_MATCH,
        NO_POLICY_EVIDENCE,
        NO_OPERATING_HOURS,
        NOT_ENOUGH_UNIQUE_CANDIDATES,
        LOW_RESULT_COUNT
    }

    public enum SuggestedAction {
        DROP_QUERY_TEXT,
        EXPAND_QUERY_TEXT,
        EXPAND_REGION,
        INCREASE_LIMIT,
        USE_OPERATING_HOURS_ESTIMATES,
        ASK_USER_TO_ADJUST_REQUIRED_CONDITION
    }

    public record Diagnostics(
            @JsonProperty("requested_limit") Integer requestedLimit,
            @JsonProperty("returned_count") Integer returnedCount,
            @JsonProperty("unique_count") Integer uniqueCount,
            Integer shortage,
            @JsonProperty("failure_reasons") List<FailureReason> failureReasons,
            Map<String, Long> counts,
            @JsonProperty("suggested_actions") List<SuggestedAction> suggestedActions,
            @JsonProperty("under_matched_preferences") List<String> underMatchedPreferences,
            @JsonProperty("unmatched_query_terms") List<String> unmatchedQueryTerms,
            @JsonProperty("missing_evidence_fields") List<String> missingEvidenceFields
    ) {
        public Diagnostics {
            failureReasons = failureReasons == null ? List.of() : failureReasons;
            counts = counts == null ? Map.of() : counts;
            suggestedActions = suggestedActions == null ? List.of() : suggestedActions;
            underMatchedPreferences = underMatchedPreferences == null ? List.of() : underMatchedPreferences;
            unmatchedQueryTerms = unmatchedQueryTerms == null ? List.of() : unmatchedQueryTerms;
            missingEvidenceFields = missingEvidenceFields == null ? List.of() : missingEvidenceFields;
        }
    }

    public record Candidate(
            @JsonProperty("place_id") String placeId,
            PlaceSearchRequest.Domain domain,
            String name,
            String address,
            @JsonProperty("region_code") String regionCode,
            @JsonProperty("region_match") Boolean regionMatch,
            @JsonProperty("place_subtype") String placeSubtype,
            @JsonProperty("pet_allowed") Boolean petAllowed,
            @JsonProperty("max_pet_size") String maxPetSize,
            @JsonProperty("wheelchair_accessible") Boolean wheelchairAccessible,
            Location location,
            @JsonProperty("distance_km") Double distanceKm,
            Double score,
            Status status,
            @JsonProperty("missing_fields") List<String> missingFields,
            @JsonProperty("matched_preferences") List<String> matchedPreferences,
            @JsonProperty("matched_keywords") List<String> matchedKeywords,
            @JsonProperty("matched_preference_details") List<MatchedPreference> matchedPreferenceDetails,
            List<Evidence> evidence
    ) {
        public Candidate {
            missingFields = missingFields == null ? List.of() : missingFields;
            matchedPreferences = matchedPreferences == null ? List.of() : matchedPreferences;
            matchedKeywords = matchedKeywords == null ? List.of() : matchedKeywords;
            matchedPreferenceDetails = matchedPreferenceDetails == null ? List.of() : matchedPreferenceDetails;
            evidence = evidence == null ? List.of() : evidence;
        }

        public Candidate(
                String placeId,
                PlaceSearchRequest.Domain domain,
                String name,
                String address,
                Location location,
                Double distanceKm,
                Double score,
                Status status,
                List<String> missingFields,
                List<String> matchedPreferences,
                List<Evidence> evidence
        ) {
            this(placeId, domain, name, address, null, null, null, null, null, null, location, distanceKm, score, status,
                    missingFields, matchedPreferences, List.of(), List.of(), evidence);
        }
    }

    public record Location(Double lat, Double lon) {
    }

    public record MatchedPreference(
            String preference,
            @JsonProperty("matched_keywords") List<String> matchedKeywords,
            List<String> fields
    ) {
        public MatchedPreference {
            matchedKeywords = matchedKeywords == null ? List.of() : matchedKeywords;
            fields = fields == null ? List.of() : fields;
        }
    }

    public record Evidence(String field, Object value, String source) {
    }
}
