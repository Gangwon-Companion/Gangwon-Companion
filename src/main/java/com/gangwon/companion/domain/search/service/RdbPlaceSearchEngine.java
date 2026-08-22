package com.gangwon.companion.domain.search.service;

import com.gangwon.companion.domain.destination.entity.AccessibilityInfo;
import com.gangwon.companion.domain.destination.entity.Destination;
import com.gangwon.companion.domain.destination.entity.PetInfo;
import com.gangwon.companion.domain.destination.repository.AccessibilityInfoRepository;
import com.gangwon.companion.domain.destination.repository.DestinationRepository;
import com.gangwon.companion.domain.destination.repository.PetInfoRepository;
import com.gangwon.companion.domain.lodging.entity.Lodging;
import com.gangwon.companion.domain.lodging.repository.LodgingRepository;
import com.gangwon.companion.domain.restaurant.entity.Restaurant;
import com.gangwon.companion.domain.restaurant.repository.RestaurantRepository;
import com.gangwon.companion.domain.search.dto.GangwonRegion;
import com.gangwon.companion.domain.search.dto.PlaceSearchRequest;
import com.gangwon.companion.domain.search.dto.PlaceSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "search.engine", havingValue = "rdb", matchIfMissing = true)
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RdbPlaceSearchEngine implements PlaceSearchEngine {
    private static final int MAX_FETCH = 500;
    private static final Map<String, List<String>> PREFERENCE_TERMS = Map.of(
            "quiet", List.of("조용", "한적"),
            "ocean_view", List.of("바다", "해변", "오션뷰"),
            "cafe", List.of("카페", "커피"),
            "oceanView", List.of("바다", "해변", "오션뷰")
    );

    private final DestinationRepository destinationRepository;
    private final PetInfoRepository petInfoRepository;
    private final AccessibilityInfoRepository accessibilityInfoRepository;
    private final RestaurantRepository restaurantRepository;
    private final LodgingRepository lodgingRepository;

    @Override
    public PlaceSearchResponse search(PlaceSearchRequest request) {
        List<PlaceSearchResponse.Candidate> candidates = searchCandidates(request, false);
        if (candidates.isEmpty() && request.queryText() != null && !request.queryText().isBlank()) {
            candidates = searchCandidates(request, true);
        }
        int limit = request.limit() == null ? 5 : Math.max(1, Math.min(request.limit(), 100));
        return new PlaceSearchResponse(candidates.stream()
                .sorted(Comparator.comparingDouble(PlaceSearchResponse.Candidate::score).reversed()
                        .thenComparing(PlaceSearchResponse.Candidate::placeId))
                .limit(limit)
                .toList());
    }

    private List<PlaceSearchResponse.Candidate> searchCandidates(PlaceSearchRequest request, boolean relaxed) {
        return switch (request.domain()) {
            case DESTINATION -> destinations(request, relaxed);
            case RESTAURANT -> restaurants(request, relaxed);
            case LODGING -> lodgings(request, relaxed);
        };
    }

    private List<PlaceSearchResponse.Candidate> destinations(PlaceSearchRequest request, boolean relaxed) {
        List<Destination> rows = destinationRepository.findAll(
                destinationSpec(request, relaxed), fetchPage(request)).getContent();
        List<Long> ids = rows.stream().map(Destination::getId).toList();
        Map<Long, PetInfo> pets = new HashMap<>();
        petInfoRepository.findAllByDestinationIdIn(ids)
                .forEach(info -> pets.putIfAbsent(info.getDestination().getId(), info));
        Map<Long, AccessibilityInfo> accessibility = new HashMap<>();
        accessibilityInfoRepository.findAllByDestinationIdIn(ids)
                .forEach(info -> accessibility.putIfAbsent(info.getDestination().getId(), info));

        List<PlaceSearchResponse.Candidate> result = new ArrayList<>();
        for (Destination row : rows) {
            PetInfo pet = pets.get(row.getId());
            AccessibilityInfo access = accessibility.get(row.getId());
            List<String> missing = new ArrayList<>();
            List<PlaceSearchResponse.Evidence> evidence = new ArrayList<>();
            if (!policyPasses(request, pet, access, missing, evidence)) continue;
            Double distance = distance(request, decimal(row.getMapY()), decimal(row.getMapX()));
            if (outsideRadius(request, distance)) continue;
            String text = join(row.getTitle(), row.getAddr1(), row.getAddr2(),
                    row.getTheme() == null ? null : row.getTheme().getName());
            result.add(candidate("DESTINATION:" + row.getId(), request.domain(), row.getTitle(),
                    join(row.getAddr1(), row.getAddr2()), decimal(row.getMapY()), decimal(row.getMapX()),
                    distance, text, request, missing, evidence));
        }
        return result;
    }

    private List<PlaceSearchResponse.Candidate> restaurants(PlaceSearchRequest request, boolean relaxed) {
        if (hasRequiredPolicy(request)) return List.of();
        List<Restaurant> rows = restaurantRepository.findAll(restaurantSpec(request, relaxed), fetchPage(request)).getContent();
        List<PlaceSearchResponse.Candidate> result = new ArrayList<>();
        for (Restaurant row : rows) {
            Double distance = distance(request, row.getLatitude(), row.getLongitude());
            if (outsideRadius(request, distance)) continue;
            result.add(candidate("RESTAURANT:" + row.getId(), request.domain(), row.getName(), row.getAddress(),
                    row.getLatitude(), row.getLongitude(), distance,
                    join(row.getName(), row.getMenuType(), row.getRegion(), row.getAddress()),
                    request, List.of(), List.of()));
        }
        return result;
    }

    private List<PlaceSearchResponse.Candidate> lodgings(PlaceSearchRequest request, boolean relaxed) {
        if (hasRequiredPolicy(request)) return List.of();
        List<Lodging> rows = lodgingRepository.findAll(lodgingSpec(request, relaxed), fetchPage(request)).getContent();
        List<PlaceSearchResponse.Candidate> result = new ArrayList<>();
        for (Lodging row : rows) {
            Double distance = distance(request, row.getLatitude(), row.getLongitude());
            if (outsideRadius(request, distance)) continue;
            result.add(candidate("LODGING:" + row.getId(), request.domain(), row.getName(), row.getAddress(),
                    row.getLatitude(), row.getLongitude(), distance,
                    join(row.getName(), row.getDescription(), row.getRegion(), row.getAddress()),
                    request, List.of(), List.of()));
        }
        return result;
    }

    private Specification<Destination> destinationSpec(PlaceSearchRequest request, boolean relaxed) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (request.regionCodes() != null && !request.regionCodes().isEmpty()) {
                predicates.add(root.get("sigunguCode").in(request.regionCodes().stream()
                        .map(code -> GangwonRegion.valueOf(code.name()).tourApiSigunguCode()).toList()));
            }
            addTextPredicates(predicates, request.queryText(), relaxed, cb,
                    root.get("title"), root.get("addr1"), root.get("addr2"));
            addGeoPredicates(predicates, request, cb, root.get("mapY"), root.get("mapX"));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private Specification<Restaurant> restaurantSpec(PlaceSearchRequest request, boolean relaxed) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            addKoreanRegions(predicates, request, root.get("region"));
            addTextPredicates(predicates, request.queryText(), relaxed, cb,
                    root.get("name"), root.get("menuType"), root.get("address"));
            addGeoPredicates(predicates, request, cb, root.get("latitude"), root.get("longitude"));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private Specification<Lodging> lodgingSpec(PlaceSearchRequest request, boolean relaxed) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            addKoreanRegions(predicates, request, root.get("region"));
            addTextPredicates(predicates, request.queryText(), relaxed, cb,
                    root.get("name"), root.get("description"), root.get("address"));
            addGeoPredicates(predicates, request, cb, root.get("latitude"), root.get("longitude"));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private <T> void addKoreanRegions(List<jakarta.persistence.criteria.Predicate> predicates,
                                      PlaceSearchRequest request,
                                      jakarta.persistence.criteria.Path<T> path) {
        if (request.regionCodes() != null && !request.regionCodes().isEmpty()) {
            predicates.add(path.in(request.regionCodes().stream()
                    .map(code -> GangwonRegion.valueOf(code.name()).koreanName()).toList()));
        }
    }

    @SafeVarargs
    private void addTextPredicates(List<jakarta.persistence.criteria.Predicate> predicates, String queryText,
                                   boolean relaxed,
                                   jakarta.persistence.criteria.CriteriaBuilder cb,
                                   jakarta.persistence.criteria.Expression<String>... fields) {
        if (queryText == null || queryText.isBlank()) return;
        List<jakarta.persistence.criteria.Predicate> terms = new ArrayList<>();
        for (String term : queryText.toLowerCase(Locale.ROOT).trim().split("\\s+")) {
            List<jakarta.persistence.criteria.Predicate> termMatches = new ArrayList<>();
            for (jakarta.persistence.criteria.Expression<String> field : fields) {
                termMatches.add(cb.like(cb.lower(field), "%" + term + "%"));
            }
            terms.add(cb.or(termMatches.toArray(jakarta.persistence.criteria.Predicate[]::new)));
        }
        if (!relaxed) {
            predicates.addAll(terms);
            return;
        }
        jakarta.persistence.criteria.Expression<Integer> matchedCount = cb.literal(0);
        for (jakarta.persistence.criteria.Predicate term : terms) {
            matchedCount = cb.sum(matchedCount, cb.<Integer>selectCase().when(term, 1).otherwise(0));
        }
        int required = Math.max(1, (int) Math.floor(terms.size() * 0.7));
        predicates.add(cb.ge(matchedCount, required));
    }

    private void addGeoPredicates(List<jakarta.persistence.criteria.Predicate> predicates,
                                  PlaceSearchRequest request,
                                  jakarta.persistence.criteria.CriteriaBuilder cb,
                                  jakarta.persistence.criteria.Expression<? extends Number> lat,
                                  jakarta.persistence.criteria.Expression<? extends Number> lon) {
        if (request.geo() == null || request.geo().center() == null) return;
        double radius = request.geo().radiusKm();
        double centerLat = request.geo().center().lat();
        double latDelta = radius / 111.0;
        double lonDelta = radius / (111.0 * Math.max(0.1, Math.cos(Math.toRadians(centerLat))));
        predicates.add(cb.between(lat.as(Double.class), centerLat - latDelta, centerLat + latDelta));
        predicates.add(cb.between(lon.as(Double.class), request.geo().center().lon() - lonDelta,
                request.geo().center().lon() + lonDelta));
    }

    private boolean policyPasses(PlaceSearchRequest request, PetInfo pet, AccessibilityInfo access,
                                 List<String> missing, List<PlaceSearchResponse.Evidence> evidence) {
        PlaceSearchRequest.HardFilters filters = request.hardFilters();
        if (filters == null) return true;
        if (Boolean.TRUE.equals(filters.petAllowed())) {
            Boolean value = pet == null ? null : pet.getPetAllowed();
            if (!Boolean.TRUE.equals(value)) return false;
            addPolicy("pet_allowed", value, missing, evidence);
        }
        if (filters.petSize() != null) {
            Boolean value = switch (filters.petSize()) {
                case SMALL -> pet == null ? null : pet.getSmallPetAllowed();
                case MEDIUM -> pet == null ? null : pet.getMediumPetAllowed();
                case LARGE -> pet == null ? null : pet.getLargePetAllowed();
            };
            if (!Boolean.TRUE.equals(value)) return false;
            addPolicy("pet_size", value, missing, evidence);
        }
        if (Boolean.TRUE.equals(filters.wheelchairAccessible())) {
            Boolean value = access == null ? null : access.getWheelchairAccessible();
            if (!Boolean.TRUE.equals(value)) return false;
            addPolicy("wheelchair_accessible", value, missing, evidence);
        }
        return true;
    }

    private void addPolicy(String field, Boolean value, List<String> missing,
                           List<PlaceSearchResponse.Evidence> evidence) {
        if (value == null) missing.add(field);
        else evidence.add(new PlaceSearchResponse.Evidence(field, value, "TOUR_API"));
    }

    private boolean hasRequiredPolicy(PlaceSearchRequest request) {
        PlaceSearchRequest.HardFilters filters = request.hardFilters();
        return filters != null && (Boolean.TRUE.equals(filters.petAllowed())
                || filters.petSize() != null
                || Boolean.TRUE.equals(filters.wheelchairAccessible()));
    }

    private PlaceSearchResponse.Candidate candidate(String id, PlaceSearchRequest.Domain domain,
            String name, String address, Double lat, Double lon, Double distance, String text,
            PlaceSearchRequest request, List<String> missing, List<PlaceSearchResponse.Evidence> evidence) {
        List<String> matched = matchedPreferences(text, request.softPreferences());
        double score = 0.1 + keywordScore(name, text, request.queryText())
                + preferenceScore(matched, request.softPreferences()) + distanceScore(distance, request);
        if (!missing.isEmpty()) score *= 0.7;
        return new PlaceSearchResponse.Candidate(id, domain, name, blankToNull(address),
                lat == null || lon == null ? null : new PlaceSearchResponse.Location(lat, lon),
                distance, Math.round(score * 1000.0) / 1000.0,
                missing.isEmpty() ? PlaceSearchResponse.Status.OK : PlaceSearchResponse.Status.INSUFFICIENT_EVIDENCE,
                List.copyOf(missing), matched, evidence);
    }

    private double keywordScore(String name, String text, String queryText) {
        if (queryText == null || queryText.isBlank()) return 0;
        double score = 0;
        String lowerName = name.toLowerCase(Locale.ROOT);
        String lowerText = text.toLowerCase(Locale.ROOT);
        for (String term : queryText.toLowerCase(Locale.ROOT).split("\\s+")) {
            if (lowerName.contains(term)) score += 0.35;
            else if (lowerText.contains(term)) score += 0.15;
        }
        return score;
    }

    private List<String> matchedPreferences(String text, Map<String, Double> preferences) {
        if (preferences == null || preferences.isEmpty()) return List.of();
        String lower = text.toLowerCase(Locale.ROOT);
        return preferences.keySet().stream().filter(key -> PREFERENCE_TERMS
                .getOrDefault(key, List.of(key)).stream().anyMatch(lower::contains)).sorted().toList();
    }

    private double preferenceScore(List<String> matched, Map<String, Double> preferences) {
        if (preferences == null) return 0;
        return matched.stream().mapToDouble(key -> preferences.getOrDefault(key, 0.0) * 0.2).sum();
    }

    private double distanceScore(Double distance, PlaceSearchRequest request) {
        if (distance == null || request.geo() == null) return 0;
        return 0.2 * Math.max(0, 1 - distance / request.geo().radiusKm());
    }

    private Double distance(PlaceSearchRequest request, Double lat, Double lon) {
        if (request.geo() == null || lat == null || lon == null) return null;
        double lat1 = Math.toRadians(request.geo().center().lat());
        double lon1 = Math.toRadians(request.geo().center().lon());
        double lat2 = Math.toRadians(lat);
        double lon2 = Math.toRadians(lon);
        double a = Math.pow(Math.sin((lat2 - lat1) / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin((lon2 - lon1) / 2), 2);
        return Math.round(2 * 6371 * Math.asin(Math.sqrt(a)) * 100.0) / 100.0;
    }

    private boolean outsideRadius(PlaceSearchRequest request, Double distance) {
        return request.geo() != null && (distance == null || distance > request.geo().radiusKm());
    }

    private PageRequest fetchPage(PlaceSearchRequest request) {
        int limit = request.limit() == null ? 5 : request.limit();
        return PageRequest.of(0, Math.min(MAX_FETCH, Math.max(50, limit * 10)), Sort.by("id"));
    }

    private Double decimal(java.math.BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    private String join(String... values) {
        return String.join(" ", java.util.Arrays.stream(values).filter(v -> v != null && !v.isBlank()).toList());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
