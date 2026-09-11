package com.gangwon.companion.domain.search.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gangwon.companion.domain.search.dto.PlaceSearchRequest;
import com.gangwon.companion.domain.search.dto.PlaceSearchResponse;
import com.gangwon.companion.domain.search.service.OperatingHours;
import com.gangwon.companion.domain.search.service.PlaceSearchEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "search.engine", havingValue = "elasticsearch")
public class ElasticsearchPlaceSearchEngine implements PlaceSearchEngine {
    private static final Map<String, List<String>> PREFERENCE_TERMS = Map.ofEntries(
            Map.entry("quiet", List.of("조용", "한적")),
            Map.entry("ocean_view", List.of("바다", "해변", "해안", "오션뷰", "항구")),
            Map.entry("oceanView", List.of("바다", "해변", "해안", "오션뷰", "항구")),
            Map.entry("cafe", List.of("카페", "커피", "라떼", "디저트")),
            Map.entry("food", List.of("맛집", "음식", "식당", "해산물", "횟집", "생선회", "모둠회", "활어회",
                    "물회", "회", "대게", "홍게", "막국수", "순두부")),
            Map.entry("nature", List.of("숲", "계곡", "호수", "산책", "둘레길", "자연", "등산", "산림", "산")),
            Map.entry("nightView", List.of("야경", "밤바다", "밤산책", "조명", "일몰", "노을")),
            Map.entry("pet", List.of("반려동물", "반려견", "애견", "동반 가능", "목줄", "입마개", "켄넬", "이동장")),
            Map.entry("petFriendly", List.of("반려동물", "반려견", "애견", "동반 가능", "목줄", "입마개", "켄넬", "이동장")),
            Map.entry("smallPet", List.of("소형견", "10kg 이하", "10kg이하")),
            Map.entry("mediumPet", List.of("중형견", "중소형견", "중·소형견", "15kg 이하", "15kg이하")),
            Map.entry("largePet", List.of("대형견", "25kg 이상", "25kg이상")),
            Map.entry("accessibility", List.of("무장애", "휠체어", "경사로", "턱이 없어", "장애인 화장실", "장애인 주차장", "엘리베이터", "보조견")),
            Map.entry("wheelchair", List.of("휠체어", "휠체어 접근 가능", "턱이 없어", "경사로", "무단차")),
            Map.entry("accessibleRestroom", List.of("장애인 화장실", "장애인화장실")),
            Map.entry("accessibleParking", List.of("장애인 주차장", "장애인 전용 주차", "장애인주차장")),
            Map.entry("elevator", List.of("엘리베이터", "장애인용 엘리베이터")),
            Map.entry("helpDog", List.of("보조견", "안내견", "동반가능"))
    );
    private static final Pattern STANDALONE_HOE = Pattern.compile("(^|[\\s,/|+()\\[\\]{}~·-])회($|[\\s,/|+()\\[\\]{}~·-])");
    private static final Pattern STANDALONE_MOUNTAIN = Pattern.compile("(^|[\\s,/|+()\\[\\]{}~·-])산($|[\\s,/|+()\\[\\]{}~·-](?!\\s*(로|길|번길)))");
    private static final Pattern NAMED_MOUNTAIN = Pattern.compile(
            "(설악산|오대산|치악산|태백산|가리왕산|두타산|함백산|민둥산|백운산|덕항산|응봉산|금강산)"
                    + "(?!\\s*(로|길|번길))(?=$|[\\s,/|+()\\[\\]{}~·-])");

    private final ElasticsearchHttpClient client;
    private final ElasticsearchProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public PlaceSearchResponse search(PlaceSearchRequest request) {
        boolean strictRequiredPolicy = hasRequiredPolicy(request);
        JsonNode response = client.post("/" + properties.getAlias() + "/_search",
                searchBody(request, false, false, strictRequiredPolicy));
        if (!request.queryText().isBlank() && response.path("hits").path("hits").isEmpty()) {
            response = client.post("/" + properties.getAlias() + "/_search",
                    searchBody(request, true, false, strictRequiredPolicy));
        }
        if (!request.queryText().isBlank() && response.path("hits").path("hits").isEmpty()) {
            response = client.post("/" + properties.getAlias() + "/_search",
                    searchBody(request, true, true, strictRequiredPolicy));
        }
        if (strictRequiredPolicy && response.path("hits").path("hits").isEmpty()) {
            response = client.post("/" + properties.getAlias() + "/_search",
                    searchBody(request, true, true, false));
        }
        List<PlaceSearchResponse.Candidate> candidates = new ArrayList<>();
        for (JsonNode hit : response.path("hits").path("hits")) candidates.add(candidate(hit, request));
        List<PlaceSearchResponse.Candidate> results = candidates.stream()
                .sorted(Comparator.comparingDouble(PlaceSearchResponse.Candidate::score).reversed()
                        .thenComparing(PlaceSearchResponse.Candidate::placeId))
                .limit(request.limit()).toList();
        PlaceSearchResponse.Diagnostics diagnostics = diagnostics(request, results);
        log.info("Elasticsearch search summary: domain={}, slot={}, totalHits={}, fetchedHits={}, returned={}, diagnostics={}",
                request.domain(), request.slot(), totalHits(response), candidates.size(), results.size(),
                diagnostics == null ? "none" : diagnostics.failureReasons());
        return new PlaceSearchResponse(results, diagnostics);
    }

    private Map<String, Object> searchBody(PlaceSearchRequest request, boolean relaxed, boolean filterOnly,
                                           boolean strictRequiredPolicy) {
        Map<String, Object> bool = boolQuery(request, relaxed, filterOnly, true, true, true, true, strictRequiredPolicy);
        int fetchSize = Math.min(500, Math.max(50, request.limit() * 10));
        Map<String, Object> functionScore = new LinkedHashMap<>();
        functionScore.put("query", Map.of("bool", bool));
        functionScore.put("functions", rankingFunctions(request));
        functionScore.put("score_mode", "sum");
        functionScore.put("boost_mode", "sum");
        return Map.of("size", fetchSize, "track_scores", true, "track_total_hits", true,
                "query", Map.of("function_score", functionScore),
                "sort", List.of(Map.of("_score", "desc"), Map.of("placeId", "asc")));
    }

    private long totalHits(JsonNode response) {
        JsonNode total = response.path("hits").path("total");
        if (total.isObject()) return total.path("value").asLong(0);
        if (total.isNumber()) return total.asLong();
        return response.path("hits").path("hits").size();
    }

    private Map<String, Object> boolQuery(PlaceSearchRequest request, boolean relaxed, boolean filterOnly,
                                          boolean includeQuery, boolean includePolicyFilters,
                                          boolean includeOperatingHours, boolean includeRegion,
                                          boolean strictRequiredPolicy) {
        List<Object> filters = new ArrayList<>();
        filters.add(Map.of("term", Map.of("domain", request.domain().name())));
        if (includeOperatingHours) {
            filters.add(Map.of("exists", Map.of("field", "opensAt")));
            filters.add(Map.of("exists", Map.of("field", "closesAt")));
        }
        if (includeRegion && !request.regionCodes().isEmpty()) {
            filters.add(Map.of("terms", Map.of("regionCode", request.regionCodes().stream().map(Enum::name).toList())));
        }
        if (request.geo() != null) {
            filters.add(Map.of("geo_distance", Map.of("distance", request.geo().radiusKm() + "km",
                    "location", Map.of("lat", request.geo().center().lat(), "lon", request.geo().center().lon()))));
        }
        if (includePolicyFilters) {
            addPolicyFilter(filters, "petAllowed", Boolean.TRUE.equals(request.hardFilters().petAllowed()), strictRequiredPolicy);
            if (request.hardFilters().petSize() != null) {
                addNotFalseFilter(filters, switch (request.hardFilters().petSize()) {
                    case SMALL -> "smallPetAllowed"; case MEDIUM -> "mediumPetAllowed"; case LARGE -> "largePetAllowed";
                }, true);
            }
            addPolicyFilter(filters, "wheelchairAccessible", Boolean.TRUE.equals(request.hardFilters().wheelchairAccessible()), strictRequiredPolicy);
        }

        Map<String, Object> bool = new LinkedHashMap<>();
        bool.put("filter", filters);
        if (includeQuery && !request.queryText().isBlank() && !filterOnly) {
            Map<String, Object> multiMatch = new LinkedHashMap<>();
            multiMatch.put("query", request.queryText());
            multiMatch.put("operator", relaxed ? "or" : "and");
            if (relaxed) multiMatch.put("minimum_should_match", "70%");
            multiMatch.put("type", "cross_fields");
            multiMatch.put("fields", List.of(
                    "name^6", "name.english^3", "themeName^3", "menuType^3", "placeSubtype^3", "address^2",
                    "searchText^1.5", "searchText.english", "petInfoText^1.2", "accessibilityInfoText^1.2"));
            bool.put("must", List.of(Map.of("multi_match", multiMatch)));
            bool.put("should", keywordBoosts(request));
        } else if (!request.softPreferences().isEmpty()) {
            bool.put("should", preferenceBoosts(request));
        }
        return bool;
    }

    private void addNotFalseFilter(List<Object> filters, String field, boolean requested) {
        if (!requested) return;
        filters.add(Map.of("bool", Map.of("must_not", List.of(Map.of("term", Map.of(field, false))))));
    }

    private void addPolicyFilter(List<Object> filters, String field, boolean requested, boolean strict) {
        if (!requested) return;
        if (strict) {
            filters.add(Map.of("term", Map.of(field, true)));
        } else {
            addNotFalseFilter(filters, field, true);
        }
    }

    private List<Object> rankingFunctions(PlaceSearchRequest request) {
        List<Object> functions = new ArrayList<>();
        functions.add(Map.of("field_value_factor", Map.of(
                "field", "rating", "factor", 0.2, "modifier", "sqrt", "missing", 0)));
        if (request.geo() != null) {
            functions.add(Map.of("gauss", Map.of("location", Map.of(
                    "origin", Map.of("lat", request.geo().center().lat(), "lon", request.geo().center().lon()),
                    "scale", Math.max(1.0, request.geo().radiusKm() / 2.0) + "km", "decay", 0.5)),
                    "weight", 0.5));
        }
        return functions;
    }

    private PlaceSearchResponse.Diagnostics diagnostics(PlaceSearchRequest request, List<PlaceSearchResponse.Candidate> results) {
        List<String> underMatchedPreferences = underMatchedPreferences(request, results);
        List<String> unmatchedQueryTerms = unmatchedQueryTerms(request, results);
        List<String> missingEvidenceFields = missingEvidenceFields(request, results);
        int uniqueCount = (int) results.stream().map(PlaceSearchResponse.Candidate::placeId).distinct().count();
        int shortage = Math.max(0, request.limit() - uniqueCount);
        if (shortage == 0 && underMatchedPreferences.isEmpty()
                && unmatchedQueryTerms.isEmpty() && missingEvidenceFields.isEmpty()) {
            return null;
        }

        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("current", count(request, true, true, true, true));
        counts.put("without_query", count(request, false, true, true, true));
        counts.put("without_policy_filters", count(request, true, false, true, true));
        counts.put("without_operating_hours", count(request, true, true, false, true));
        counts.put("region_only", count(request, false, false, false, true));
        counts.put("domain_only", count(request, false, false, false, false));

        List<PlaceSearchResponse.FailureReason> reasons = failureReasons(
                request, results.size(), uniqueCount, counts, missingEvidenceFields);
        List<PlaceSearchResponse.SuggestedAction> actions = suggestedActions(reasons);
        return new PlaceSearchResponse.Diagnostics(request.limit(), results.size(), uniqueCount, shortage,
                reasons, counts, actions, underMatchedPreferences, unmatchedQueryTerms, missingEvidenceFields);
    }

    private long count(PlaceSearchRequest request, boolean includeQuery, boolean includePolicyFilters,
                       boolean includeOperatingHours, boolean includeRegion) {
        Map<String, Object> body = Map.of("query", Map.of("bool",
                boolQuery(request, true, false, includeQuery, includePolicyFilters, includeOperatingHours, includeRegion,
                        hasRequiredPolicy(request))));
        JsonNode response = client.post("/" + properties.getAlias() + "/_count", body);
        return response == null ? 0 : response.path("count").asLong(0);
    }

    private List<PlaceSearchResponse.FailureReason> failureReasons(
            PlaceSearchRequest request, int returnedCount, int uniqueCount, Map<String, Long> counts,
            List<String> missingEvidenceFields) {
        Set<PlaceSearchResponse.FailureReason> reasons = new LinkedHashSet<>();
        long current = counts.getOrDefault("current", 0L);
        long withoutQuery = counts.getOrDefault("without_query", current);
        long withoutPolicy = counts.getOrDefault("without_policy_filters", current);
        long withoutOperating = counts.getOrDefault("without_operating_hours", current);
        long regionOnly = counts.getOrDefault("region_only", current);
        long domainOnly = counts.getOrDefault("domain_only", current);

        if (!request.regionCodes().isEmpty() && regionOnly == 0 && domainOnly > 0) {
            reasons.add(PlaceSearchResponse.FailureReason.NO_REGION_MATCH);
        }
        if (!request.queryText().isBlank() && withoutQuery > current) {
            reasons.add(PlaceSearchResponse.FailureReason.NO_TEXT_MATCH);
        }
        if (hasPolicyRequest(request) && withoutPolicy > current && hasMissingRequiredPolicy(missingEvidenceFields)) {
            reasons.add(PlaceSearchResponse.FailureReason.NO_POLICY_EVIDENCE);
        }
        if (withoutOperating > current) {
            reasons.add(PlaceSearchResponse.FailureReason.NO_OPERATING_HOURS);
        }
        if (uniqueCount < returnedCount || (returnedCount > 0 && uniqueCount < request.limit())) {
            reasons.add(PlaceSearchResponse.FailureReason.NOT_ENOUGH_UNIQUE_CANDIDATES);
        }
        if (returnedCount < request.limit()) {
            reasons.add(PlaceSearchResponse.FailureReason.LOW_RESULT_COUNT);
        }
        return new ArrayList<>(reasons);
    }

    private boolean hasPolicyRequest(PlaceSearchRequest request) {
        return Boolean.TRUE.equals(request.hardFilters().petAllowed())
                || Boolean.TRUE.equals(request.hardFilters().wheelchairAccessible());
    }

    private boolean hasRequiredPolicy(PlaceSearchRequest request) {
        return hasPolicyRequest(request);
    }

    private boolean hasMissingRequiredPolicy(List<String> missingEvidenceFields) {
        return missingEvidenceFields.contains("pet_allowed")
                || missingEvidenceFields.contains("wheelchair_accessible");
    }

    private List<PlaceSearchResponse.SuggestedAction> suggestedActions(List<PlaceSearchResponse.FailureReason> reasons) {
        Set<PlaceSearchResponse.SuggestedAction> actions = new LinkedHashSet<>();
        if (reasons.contains(PlaceSearchResponse.FailureReason.NO_TEXT_MATCH)) {
            actions.add(PlaceSearchResponse.SuggestedAction.EXPAND_QUERY_TEXT);
            actions.add(PlaceSearchResponse.SuggestedAction.DROP_QUERY_TEXT);
        }
        if (reasons.contains(PlaceSearchResponse.FailureReason.NO_REGION_MATCH)) {
            actions.add(PlaceSearchResponse.SuggestedAction.EXPAND_REGION);
        }
        if (reasons.contains(PlaceSearchResponse.FailureReason.NO_OPERATING_HOURS)) {
            actions.add(PlaceSearchResponse.SuggestedAction.USE_OPERATING_HOURS_ESTIMATES);
        }
        if (reasons.contains(PlaceSearchResponse.FailureReason.NOT_ENOUGH_UNIQUE_CANDIDATES)
                || reasons.contains(PlaceSearchResponse.FailureReason.LOW_RESULT_COUNT)) {
            actions.add(PlaceSearchResponse.SuggestedAction.INCREASE_LIMIT);
        }
        if (reasons.contains(PlaceSearchResponse.FailureReason.NO_POLICY_EVIDENCE)) {
            actions.add(PlaceSearchResponse.SuggestedAction.ASK_USER_TO_ADJUST_REQUIRED_CONDITION);
        }
        return new ArrayList<>(actions);
    }

    private List<Object> keywordBoosts(PlaceSearchRequest request) {
        List<Object> boosts = new ArrayList<>();
        boosts.add(Map.of("term", Map.of("name.raw", Map.of("value", request.queryText(), "boost", 12.0))));
        boosts.add(Map.of("match_phrase", Map.of("name", Map.of("query", request.queryText(), "boost", 7.0))));
        boosts.addAll(preferenceBoosts(request));
        return boosts;
    }

    private List<Object> preferenceBoosts(PlaceSearchRequest request) {
        List<Object> boosts = new ArrayList<>();
        request.softPreferences().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            List<String> terms = PREFERENCE_TERMS.getOrDefault(entry.getKey(), List.of(entry.getKey()));
            for (String term : terms) {
                for (PreferenceField field : preferenceFields(entry.getKey())) {
                    boosts.add(Map.of("match", Map.of(field.name(), Map.of(
                            "query", term, "boost", Math.max(0.0, entry.getValue()) * field.weight()))));
                }
            }
        });
        return boosts;
    }

    private PlaceSearchResponse.Candidate candidate(JsonNode hit, PlaceSearchRequest request) {
        try {
            PlaceSearchDocument doc = objectMapper.treeToValue(hit.path("_source"), PlaceSearchDocument.class);
            List<String> missing = missingFields(doc, request);
            List<PlaceSearchResponse.Evidence> evidence = evidence(doc, request);
            MatchedPreferenceAnalysis matched = matchedPreferenceAnalysis(doc, request);
            double baseScore = hit.path("_score").isNumber() ? hit.path("_score").asDouble() : 0;
            Double distance = distance(request, doc.location());
            double score = Math.round((baseScore + distanceScore(distance, request)) * 1000.0) / 1000.0;
            return new PlaceSearchResponse.Candidate(doc.placeId(), PlaceSearchRequest.Domain.valueOf(doc.domain()),
                    doc.name(), doc.address(), doc.regionCode(), regionMatch(doc, request), doc.placeSubtype(),
                    doc.petAllowed(), maxPetSize(doc), doc.wheelchairAccessible(), doc.location() == null ? null
                    : new PlaceSearchResponse.Location(doc.location().lat(), doc.location().lon()), distance, score,
                    missing.isEmpty() ? PlaceSearchResponse.Status.OK : PlaceSearchResponse.Status.INSUFFICIENT_EVIDENCE,
                    missing, matched.preferences(), matched.keywords(), matched.details(), evidence);
        } catch (Exception exception) {
            throw new ElasticsearchOperationException("Invalid search hit", exception);
        }
    }

    private List<String> missingFields(PlaceSearchDocument doc, PlaceSearchRequest request) {
        List<String> missing = new ArrayList<>();
        if (doc.opensAt() == null || doc.closesAt() == null) missing.add("operating_hours");
        if (Boolean.TRUE.equals(request.hardFilters().petAllowed()) && doc.petAllowed() == null) missing.add("pet_allowed");
        if (Boolean.TRUE.equals(request.hardFilters().wheelchairAccessible()) && doc.wheelchairAccessible() == null) {
            missing.add("wheelchair_accessible");
        }
        return missing;
    }

    private List<PlaceSearchResponse.Evidence> evidence(PlaceSearchDocument doc, PlaceSearchRequest request) {
        List<PlaceSearchResponse.Evidence> evidence = new ArrayList<>();
        if (doc.opensAt() != null && doc.closesAt() != null) {
            evidence.add(new PlaceSearchResponse.Evidence("opens_at", doc.opensAt(), doc.source()));
            evidence.add(new PlaceSearchResponse.Evidence("closes_at", doc.closesAt(), doc.source()));
            OperatingHours.parse(doc.operatingHoursRaw())
                    .filter(OperatingHours.Range::estimated)
                    .ifPresent(ignored -> {
                        evidence.add(new PlaceSearchResponse.Evidence("operating_hours_estimated", true, doc.source()));
                        evidence.add(new PlaceSearchResponse.Evidence("operating_hours_raw", doc.operatingHoursRaw(), doc.source()));
                    });
        }
        if (Boolean.TRUE.equals(request.hardFilters().petAllowed()) && doc.petAllowed() != null) {
            evidence.add(new PlaceSearchResponse.Evidence("pet_allowed", doc.petAllowed(), doc.source()));
        }
        if (maxPetSize(doc) != null) {
            evidence.add(new PlaceSearchResponse.Evidence("max_pet_size", maxPetSize(doc), doc.source()));
        }
        if (Boolean.TRUE.equals(request.hardFilters().wheelchairAccessible()) && doc.wheelchairAccessible() != null) {
            evidence.add(new PlaceSearchResponse.Evidence("wheelchair_accessible", doc.wheelchairAccessible(), doc.source()));
        }
        return evidence;
    }

    private Boolean petSize(PlaceSearchDocument doc, PlaceSearchRequest.PetSize size) {
        return switch (size) { case SMALL -> doc.smallPetAllowed(); case MEDIUM -> doc.mediumPetAllowed(); case LARGE -> doc.largePetAllowed(); };
    }

    private String maxPetSize(PlaceSearchDocument doc) {
        if (Boolean.TRUE.equals(doc.largePetAllowed())) return PlaceSearchRequest.PetSize.LARGE.name();
        if (Boolean.TRUE.equals(doc.mediumPetAllowed())) return PlaceSearchRequest.PetSize.MEDIUM.name();
        if (Boolean.TRUE.equals(doc.smallPetAllowed())) return PlaceSearchRequest.PetSize.SMALL.name();
        return null;
    }

    private MatchedPreferenceAnalysis matchedPreferenceAnalysis(PlaceSearchDocument doc, PlaceSearchRequest request) {
        if (request.softPreferences() == null || request.softPreferences().isEmpty()) {
            return new MatchedPreferenceAnalysis(List.of(), matchedQueryTerms(doc, request), List.of());
        }
        List<String> preferences = new ArrayList<>();
        Set<String> keywords = new LinkedHashSet<>(matchedQueryTerms(doc, request));
        List<PlaceSearchResponse.MatchedPreference> details = new ArrayList<>();
        Map<String, String> texts = preferenceFieldTexts(doc);

        request.softPreferences().keySet().stream().sorted().forEach(key -> {
            Set<String> matchedTerms = new LinkedHashSet<>();
            Set<String> matchedFields = new LinkedHashSet<>();
            for (String term : PREFERENCE_TERMS.getOrDefault(key, List.of(key))) {
                for (Map.Entry<String, String> field : texts.entrySet()) {
                    if (termMatches(field.getValue(), term)) {
                        matchedTerms.add(term);
                        matchedFields.add(field.getKey());
                    }
                }
            }
            if (!matchedTerms.isEmpty()) {
                preferences.add(key);
                keywords.addAll(matchedTerms);
                details.add(new PlaceSearchResponse.MatchedPreference(
                        key, new ArrayList<>(matchedTerms), new ArrayList<>(matchedFields)));
            }
        });
        return new MatchedPreferenceAnalysis(preferences, new ArrayList<>(keywords), details);
    }

    private Stream<String> preferenceTerms(String key) {
        return PREFERENCE_TERMS.getOrDefault(key, List.of(key)).stream()
                .map(value -> value.toLowerCase(Locale.ROOT));
    }

    private List<PreferenceField> preferenceFields(String key) {
        if (isPetPreference(key)) return List.of(new PreferenceField("petInfoText", 2.5), new PreferenceField("searchText", 1.0));
        if (isAccessibilityPreference(key)) return List.of(new PreferenceField("accessibilityInfoText", 2.5), new PreferenceField("searchText", 1.0));
        return List.of(new PreferenceField("searchText", 1.0));
    }

    private boolean isPetPreference(String key) {
        return List.of("pet", "petFriendly", "smallPet", "mediumPet", "largePet").contains(key);
    }

    private boolean isAccessibilityPreference(String key) {
        return List.of("accessibility", "wheelchair", "accessibleRestroom", "accessibleParking",
                "elevator", "helpDog").contains(key);
    }

    private String preferenceText(PlaceSearchDocument doc) {
        return Stream.of(doc.searchText(), doc.petInfoText(), doc.accessibilityInfoText())
                .filter(value -> value != null && !value.isBlank())
                .reduce("", (left, right) -> left + " " + right);
    }

    private Map<String, String> preferenceFieldTexts(PlaceSearchDocument doc) {
        Map<String, String> texts = new LinkedHashMap<>();
        texts.put("name", normalize(doc.name()));
        texts.put("searchText", normalize(doc.searchText()));
        texts.put("themeName", normalize(doc.themeName()));
        texts.put("menuType", normalize(doc.menuType()));
        texts.put("placeSubtype", normalize(doc.placeSubtype()));
        texts.put("petInfoText", normalize(doc.petInfoText()));
        texts.put("accessibilityInfoText", normalize(doc.accessibilityInfoText()));
        return texts;
    }

    private List<String> matchedQueryTerms(PlaceSearchDocument doc, PlaceSearchRequest request) {
        String text = normalize(preferenceText(doc));
        return queryTerms(request).stream().filter(term -> termMatches(text, term)).toList();
    }

    private List<String> queryTerms(PlaceSearchRequest request) {
        if (request.queryText() == null || request.queryText().isBlank()) return List.of();
        return java.util.Arrays.stream(request.queryText().split("[\\s,./|+()\\[\\]{}]+"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private List<String> underMatchedPreferences(PlaceSearchRequest request, List<PlaceSearchResponse.Candidate> results) {
        if (request.softPreferences() == null || request.softPreferences().isEmpty()) return List.of();
        Set<String> matched = results.stream()
                .flatMap(candidate -> candidate.matchedPreferences().stream())
                .collect(java.util.stream.Collectors.toSet());
        return request.softPreferences().keySet().stream()
                .filter(key -> !matched.contains(key))
                .sorted()
                .toList();
    }

    private List<String> unmatchedQueryTerms(PlaceSearchRequest request, List<PlaceSearchResponse.Candidate> results) {
        List<String> terms = queryTerms(request);
        if (terms.isEmpty()) return List.of();
        Set<String> matched = results.stream()
                .flatMap(candidate -> candidate.matchedKeywords().stream())
                .map(this::normalize)
                .collect(java.util.stream.Collectors.toSet());
        return terms.stream()
                .filter(term -> !matched.contains(normalize(term)))
                .toList();
    }

    private List<String> missingEvidenceFields(PlaceSearchRequest request, List<PlaceSearchResponse.Candidate> results) {
        if (results.isEmpty()) {
            Set<String> missing = new LinkedHashSet<>();
            missing.add("operating_hours");
            if (Boolean.TRUE.equals(request.hardFilters().petAllowed())) missing.add("pet_allowed");
            if (Boolean.TRUE.equals(request.hardFilters().wheelchairAccessible())) missing.add("wheelchair_accessible");
            return new ArrayList<>(missing);
        }
        Set<String> commonMissing = new LinkedHashSet<>(results.get(0).missingFields());
        results.stream()
                .skip(1)
                .map(candidate -> new LinkedHashSet<>(candidate.missingFields()))
                .forEach(commonMissing::retainAll);
        return new ArrayList<>(commonMissing);
    }

    private boolean regionMatch(PlaceSearchDocument doc, PlaceSearchRequest request) {
        if (doc.regionCode() == null || request.regionCodes().isEmpty()) return true;
        return request.regionCodes().stream().map(Enum::name).anyMatch(doc.regionCode()::equals);
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private boolean termMatches(String normalizedText, String term) {
        if (normalizedText == null || normalizedText.isBlank() || term == null || term.isBlank()) return false;
        String normalizedTerm = normalize(term);
        return switch (normalizedTerm) {
            case "회" -> hoeMatches(normalizedText);
            case "산" -> mountainMatches(normalizedText);
            default -> normalizedText.contains(normalizedTerm);
        };
    }

    private boolean hoeMatches(String normalizedText) {
        return STANDALONE_HOE.matcher(normalizedText).find()
                || containsAny(normalizedText, "횟집", "회센터", "회센타", "생선회", "모둠회", "활어회", "광어회",
                "우럭회", "오징어회", "물회", "회덮밥", "회 맛집", "회 전문");
    }

    private boolean mountainMatches(String normalizedText) {
        return STANDALONE_MOUNTAIN.matcher(normalizedText).find()
                || NAMED_MOUNTAIN.matcher(normalizedText).find()
                || containsAny(normalizedText, "등산", "산 정상", "산정상", "산림", "산자락", "산길", "산속");
    }

    private boolean containsAny(String normalizedText, String... terms) {
        for (String term : terms) {
            if (normalizedText.contains(normalize(term))) return true;
        }
        return false;
    }

    private record MatchedPreferenceAnalysis(
            List<String> preferences,
            List<String> keywords,
            List<PlaceSearchResponse.MatchedPreference> details
    ) {
    }

    private record PreferenceField(String name, double weight) {
    }

    private Double distance(PlaceSearchRequest request, PlaceSearchDocument.Location location) {
        if (request.geo() == null || location == null) return null;
        double lat1 = Math.toRadians(request.geo().center().lat());
        double lon1 = Math.toRadians(request.geo().center().lon());
        double lat2 = Math.toRadians(location.lat());
        double lon2 = Math.toRadians(location.lon());
        double a = Math.pow(Math.sin((lat2 - lat1) / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin((lon2 - lon1) / 2), 2);
        return Math.round(2 * 6371 * Math.asin(Math.sqrt(a)) * 100.0) / 100.0;
    }

    private double distanceScore(Double distance, PlaceSearchRequest request) {
        if (distance == null || request.geo() == null) return 0;
        return 0.2 * Math.max(0, 1 - distance / request.geo().radiusKm());
    }
}
