package com.gangwon.companion.domain.search.elasticsearch;

import com.gangwon.companion.domain.destination.entity.AccessibilityInfo;
import com.gangwon.companion.domain.destination.entity.Destination;
import com.gangwon.companion.domain.destination.entity.DestinationDetail;
import com.gangwon.companion.domain.destination.entity.PetInfo;
import com.gangwon.companion.domain.destination.repository.AccessibilityInfoRepository;
import com.gangwon.companion.domain.destination.repository.DestinationRepository;
import com.gangwon.companion.domain.destination.repository.DestinationDetailRepository;
import com.gangwon.companion.domain.destination.repository.PetInfoRepository;
import com.gangwon.companion.domain.lodging.repository.LodgingRepository;
import com.gangwon.companion.domain.restaurant.repository.RestaurantRepository;
import com.gangwon.companion.domain.search.dto.GangwonRegion;
import com.gangwon.companion.domain.search.service.OperatingHours;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PlaceSearchDocumentAssembler {
    private static final int DOCUMENT_VERSION = 7;

    private final DestinationRepository destinationRepository;
    private final DestinationDetailRepository destinationDetailRepository;
    private final PetInfoRepository petInfoRepository;
    private final AccessibilityInfoRepository accessibilityInfoRepository;
    private final RestaurantRepository restaurantRepository;
    private final LodgingRepository lodgingRepository;

    @Transactional(readOnly = true)
    public List<PlaceSearchDocument> loadAll() {
        List<PlaceSearchDocument> documents = new ArrayList<>();
        documents.addAll(destinations());
        restaurantRepository.findAll().stream().map(row -> new PlaceSearchDocument(
                "RESTAURANT:" + row.getId(), "RESTAURANT", row.getName(), row.getAddress(),
                regionCode(row.getRegion()), restaurantSearchText(row),
                location(row.getLatitude(), row.getLongitude()),
                null, null, null, null, null, null, row.getMenuType(), restaurantSubtype(row), row.getRating(), null, null,
                null, opens(row.getOpenTime()), closes(row.getOpenTime()), row.getOpenTime(),
                timestamp(row.getCreatedAt()), DOCUMENT_VERSION, "TOUR_API", evidence(row.getOpenTime()))).forEach(documents::add);
        lodgingRepository.findAll().stream().map(row -> new PlaceSearchDocument(
                "LODGING:" + row.getId(), "LODGING", row.getName(), row.getAddress(),
                regionCode(row.getRegion()), lodgingSearchText(row),
                location(row.getLatitude(), row.getLongitude()),
                null, null, null, null, null, null, null, "LODGING", row.getRating(), row.getPrice(), null,
                null, opens(hours(row.getCheckInTime(), row.getCheckOutTime())),
                closes(hours(row.getCheckInTime(), row.getCheckOutTime())),
                hours(row.getCheckInTime(), row.getCheckOutTime()), timestamp(row.getCreatedAt()),
                DOCUMENT_VERSION, "TOUR_API", evidence(hours(row.getCheckInTime(), row.getCheckOutTime())))).forEach(documents::add);
        return documents;
    }

    @Transactional(readOnly = true)
    public Optional<PlaceSearchDocument> loadOne(String domain, long id) {
        return switch (domain) {
            case "DESTINATION" -> destinationRepository.findById(id).map(this::destination);
            case "RESTAURANT" -> restaurantRepository.findById(id).map(row -> new PlaceSearchDocument(
                    "RESTAURANT:" + row.getId(), "RESTAURANT", row.getName(), row.getAddress(),
                    regionCode(row.getRegion()), restaurantSearchText(row),
                    location(row.getLatitude(), row.getLongitude()),
                    null, null, null, null, null, null, row.getMenuType(), restaurantSubtype(row), row.getRating(), null, null,
                    null, opens(row.getOpenTime()), closes(row.getOpenTime()), row.getOpenTime(),
                    timestamp(row.getCreatedAt()), DOCUMENT_VERSION, "TOUR_API", evidence(row.getOpenTime())));
            case "LODGING" -> lodgingRepository.findById(id).map(row -> new PlaceSearchDocument(
                    "LODGING:" + row.getId(), "LODGING", row.getName(), row.getAddress(),
                    regionCode(row.getRegion()), lodgingSearchText(row),
                    location(row.getLatitude(), row.getLongitude()),
                    null, null, null, null, null, null, null, "LODGING", row.getRating(), row.getPrice(), null,
                    null, opens(hours(row.getCheckInTime(), row.getCheckOutTime())),
                    closes(hours(row.getCheckInTime(), row.getCheckOutTime())),
                    hours(row.getCheckInTime(), row.getCheckOutTime()), timestamp(row.getCreatedAt()),
                    DOCUMENT_VERSION, "TOUR_API", evidence(hours(row.getCheckInTime(), row.getCheckOutTime()))));
            default -> throw new IllegalArgumentException("Unsupported place domain: " + domain);
        };
    }

    private List<PlaceSearchDocument> destinations() {
        List<Destination> rows = destinationRepository.findAll();
        List<Long> ids = rows.stream().map(Destination::getId).toList();
        Map<Long, PetInfo> pets = firstPetByDestination(ids);
        Map<Long, AccessibilityInfo> access = firstAccessByDestination(ids);
        Map<Long, String> overviews = overviewsByDestination(ids);
        return rows.stream().map(row -> {
            PetInfo pet = pets.get(row.getId());
            AccessibilityInfo accessibility = access.get(row.getId());
            List<String> evidence = new ArrayList<>();
            String theme = row.getTheme() == null ? null : row.getTheme().getName();
            String petInfoText = petInfoText(pet);
            String accessibilityInfoText = accessibilityInfoText(accessibility);
            Boolean petAllowed = petAllowed(pet, petInfoText);
            Boolean smallPetAllowed = petSizeAllowed(pet, petInfoText, PetSize.SMALL);
            Boolean mediumPetAllowed = petSizeAllowed(pet, petInfoText, PetSize.MEDIUM);
            Boolean largePetAllowed = petSizeAllowed(pet, petInfoText, PetSize.LARGE);
            Boolean wheelchairAccessible = wheelchairAccessible(accessibility, accessibilityInfoText);
            if (petAllowed != null) evidence.add("pet_allowed");
            if (smallPetAllowed != null || mediumPetAllowed != null || largePetAllowed != null) evidence.add("pet_size");
            if (wheelchairAccessible != null) evidence.add("wheelchair_accessible");
            String operatingHours = operatingHoursByDestination(List.of(row.getId())).get(row.getId());
            Optional<OperatingHours.Range> parsedHours = OperatingHours.parse(operatingHours);
            if (parsedHours.isPresent()) {
                evidence.add("opens_at");
                evidence.add("closes_at");
                if (parsedHours.get().estimated()) evidence.add("operating_hours_estimated");
            }
            return new PlaceSearchDocument("DESTINATION:" + row.getId(), "DESTINATION", row.getTitle(),
                    join(row.getAddr1(), row.getAddr2()), regionCodeFromTour(row.getSigunguCode()),
                    join(row.getTitle(), row.getAddr1(), row.getAddr2(), theme, overviews.get(row.getId()),
                            petInfoText, accessibilityInfoText), location(row),
                    petAllowed, smallPetAllowed, mediumPetAllowed, largePetAllowed,
                    wheelchairAccessible, theme, null, "DESTINATION", null, null,
                    petInfoText, accessibilityInfoText, opens(operatingHours), closes(operatingHours), operatingHours,
                    timestamp(row.getUpdatedAt()), DOCUMENT_VERSION,
                    "TOUR_API", evidence);
        }).toList();
    }

    private PlaceSearchDocument destination(Destination row) {
        long id = row.getId();
        PetInfo pet = firstPetByDestination(List.of(id)).get(id);
        AccessibilityInfo accessibility = firstAccessByDestination(List.of(id)).get(id);
        String overview = overviewsByDestination(List.of(id)).get(id);
        List<String> evidence = new ArrayList<>();
        String theme = row.getTheme() == null ? null : row.getTheme().getName();
        String petInfoText = petInfoText(pet);
        String accessibilityInfoText = accessibilityInfoText(accessibility);
        Boolean petAllowed = petAllowed(pet, petInfoText);
        Boolean smallPetAllowed = petSizeAllowed(pet, petInfoText, PetSize.SMALL);
        Boolean mediumPetAllowed = petSizeAllowed(pet, petInfoText, PetSize.MEDIUM);
        Boolean largePetAllowed = petSizeAllowed(pet, petInfoText, PetSize.LARGE);
        Boolean wheelchairAccessible = wheelchairAccessible(accessibility, accessibilityInfoText);
        if (petAllowed != null) evidence.add("pet_allowed");
        if (smallPetAllowed != null || mediumPetAllowed != null || largePetAllowed != null) evidence.add("pet_size");
        if (wheelchairAccessible != null) evidence.add("wheelchair_accessible");
        String operatingHours = operatingHoursByDestination(List.of(id)).get(id);
        Optional<OperatingHours.Range> parsedHours = OperatingHours.parse(operatingHours);
        if (parsedHours.isPresent()) {
            evidence.add("opens_at");
            evidence.add("closes_at");
            if (parsedHours.get().estimated()) evidence.add("operating_hours_estimated");
        }
        return new PlaceSearchDocument("DESTINATION:" + id, "DESTINATION", row.getTitle(),
                join(row.getAddr1(), row.getAddr2()), regionCodeFromTour(row.getSigunguCode()),
                join(row.getTitle(), row.getAddr1(), row.getAddr2(), theme, overview, petInfoText, accessibilityInfoText),
                location(row), petAllowed, smallPetAllowed, mediumPetAllowed, largePetAllowed,
                wheelchairAccessible, theme, null, "DESTINATION", null, null,
                petInfoText, accessibilityInfoText, opens(operatingHours), closes(operatingHours), operatingHours,
                timestamp(row.getUpdatedAt()), DOCUMENT_VERSION, "TOUR_API", evidence);
    }

    private String restaurantSearchText(com.gangwon.companion.domain.restaurant.entity.Restaurant row) {
        return join(row.getName(), row.getMenuType(), row.getFirstMenu(), row.getTreatMenu(),
                row.getRegion(), row.getAddress(), row.getParking());
    }

    private String lodgingSearchText(com.gangwon.companion.domain.lodging.entity.Lodging row) {
        return join(row.getName(), row.getDescription(), row.getRoomType(), row.getSubFacility(),
                row.getParking(), row.getRegion(), row.getAddress());
    }

    private String restaurantSubtype(com.gangwon.companion.domain.restaurant.entity.Restaurant row) {
        String identity = join(row.getMenuType(), row.getName()).toLowerCase(Locale.ROOT);
        String menu = join(row.getFirstMenu(), row.getTreatMenu()).toLowerCase(Locale.ROOT);
        if (containsAny(identity, "베이커리", "빵집", "제과")) return "BAKERY";
        if (containsAny(identity, "디저트", "젤라또", "아이스크림", "빙수", "케이크")) return "DESSERT";
        if (containsAny(identity, "카페", "커피")) return "CAFE";
        if (containsAny(menu, "베이커리", "빵", "제과")) return "BAKERY";
        if (containsAny(menu, "디저트", "젤라또", "아이스크림", "빙수", "케이크")) return "DESSERT";
        return "RESTAURANT";
    }

    private Map<Long, String> overviewsByDestination(List<Long> ids) {
        Map<Long, List<String>> grouped = new HashMap<>();
        if (ids.isEmpty()) return Map.of();
        for (DestinationDetail detail : destinationDetailRepository.findAllByDestinationIdIn(ids)) {
            if (detail.getOverview() != null && !detail.getOverview().isBlank()) {
                grouped.computeIfAbsent(detail.getDestination().getId(), ignored -> new ArrayList<>())
                        .add(detail.getOverview());
            }
        }
        Map<Long, String> result = new HashMap<>();
        grouped.forEach((id, values) -> result.put(id, String.join(" ", values)));
        return result;
    }

    private Map<Long, String> operatingHoursByDestination(List<Long> ids) {
        Map<Long, String> result = new HashMap<>();
        if (ids.isEmpty()) return result;
        destinationDetailRepository.findAllByDestinationIdIn(ids).stream()
                .filter(detail -> detail.getUsageTime() != null && !detail.getUsageTime().isBlank())
                .forEach(detail -> result.putIfAbsent(detail.getDestination().getId(), detail.getUsageTime()));
        return result;
    }

    private String opens(String raw) {
        return OperatingHours.parse(raw).map(OperatingHours.Range::opensAt).orElse(null);
    }

    private String closes(String raw) {
        return OperatingHours.parse(raw).map(OperatingHours.Range::closesAt).orElse(null);
    }

    private String hours(String opens, String closes) {
        return join(opens, closes);
    }

    private List<String> evidence(String raw) {
        return OperatingHours.parse(raw)
                .map(range -> range.estimated()
                        ? List.of("opens_at", "closes_at", "operating_hours_estimated")
                        : List.of("opens_at", "closes_at"))
                .orElse(List.of());
    }

    private Map<Long, PetInfo> firstPetByDestination(List<Long> ids) {
        Map<Long, PetInfo> result = new HashMap<>();
        if (ids.isEmpty()) return result;
        petInfoRepository.findAllByDestinationIdIn(ids).stream().sorted(Comparator.comparing(PetInfo::getId))
                .forEach(value -> result.putIfAbsent(value.getDestination().getId(), value));
        return result;
    }

    private Map<Long, AccessibilityInfo> firstAccessByDestination(List<Long> ids) {
        Map<Long, AccessibilityInfo> result = new HashMap<>();
        if (ids.isEmpty()) return result;
        accessibilityInfoRepository.findAllByDestinationIdIn(ids).stream().sorted(Comparator.comparing(AccessibilityInfo::getId))
                .forEach(value -> result.putIfAbsent(value.getDestination().getId(), value));
        return result;
    }

    private Boolean petAllowed(PetInfo pet, String text) {
        if (pet == null) return null;
        if (pet.getPetAllowed() != null) return pet.getPetAllowed();
        if (containsAny(text, "동반 불가", "동반불가", "입장 불가", "입장불가", "출입 불가", "출입불가",
                "반려동물 금지", "반려견 금지", "동반 금지", "동반금지")) {
            return false;
        }
        return true;
    }

    private Boolean petSizeAllowed(PetInfo pet, String text, PetSize size) {
        if (pet == null) return null;
        Boolean normalized = switch (size) {
            case SMALL -> pet.getSmallPetAllowed();
            case MEDIUM -> pet.getMediumPetAllowed();
            case LARGE -> pet.getLargePetAllowed();
        };
        if (normalized != null) return normalized;
        if (containsAny(text, deniedTerms(size))) return false;
        if (containsAny(text, allowedTerms(size))) return true;
        if (size == PetSize.SMALL && containsAny(text, "중소형견", "중·소형견", "중/소형견")) return true;
        if (size == PetSize.MEDIUM && containsAny(text, "중소형견", "중·소형견", "중/소형견")) return true;
        if (containsAny(text, "크기 제한 없음", "견종 제한 없음", "전 견종", "모든 견종")) return true;
        if (size != PetSize.LARGE && containsAny(text, "대형견까지 가능", "대형견까지가능", "25kg 이상까지 가능")) {
            return true;
        }
        return null;
    }

    private Boolean wheelchairAccessible(AccessibilityInfo accessibility, String text) {
        if (accessibility == null) return null;
        if (accessibility.getWheelchairAccessible() != null) return accessibility.getWheelchairAccessible();
        if (containsAny(text, "휠체어 접근 불가", "휠체어접근불가", "휠체어 출입 불가", "휠체어출입불가",
                "휠체어 이용 불가", "휠체어이용불가", "경사로 없음", "계단만 이용", "진입 불가")) {
            return false;
        }
        if (containsAny(text, "휠체어 접근 가능", "휠체어접근가능", "휠체어 출입 가능", "휠체어출입가능",
                "휠체어 이용 가능", "휠체어이용가능", "턱이 없어", "턱이 없음", "경사로", "무단차",
                "단차 없음", "장애인용 엘리베이터")) {
            return true;
        }
        return null;
    }

    private String petInfoText(PetInfo pet) {
        if (pet == null) return null;
        return join(pet.getAccompanyType(), pet.getNeedItems(), pet.getPetFacilities(), pet.getCaution(), pet.getAccidentRisk());
    }

    private String accessibilityInfoText(AccessibilityInfo accessibility) {
        if (accessibility == null) return null;
        return join(accessibility.getParking(), accessibility.getRoute(), accessibility.getEntrance(), accessibility.getElevator(),
                accessibility.getRestroom(), accessibility.getWheelchair(), accessibility.getBraileBlock(),
                accessibility.getHelpDog(), accessibility.getGuideHuman());
    }

    private boolean containsAny(String text, String... terms) {
        if (text == null || text.isBlank()) return false;
        String normalized = text.toLowerCase(Locale.ROOT);
        String compact = normalized.replaceAll("\\s+", "");
        for (String term : terms) {
            String normalizedTerm = term.toLowerCase(Locale.ROOT);
            if (normalized.contains(normalizedTerm) || compact.contains(normalizedTerm.replaceAll("\\s+", ""))) {
                return true;
            }
        }
        return false;
    }

    private String[] allowedTerms(PetSize size) {
        return switch (size) {
            case SMALL -> new String[]{"소형견", "10kg 이하", "10kg이하"};
            case MEDIUM -> new String[]{"중형견", "중형견까지 가능", "15kg 이하", "15kg이하"};
            case LARGE -> new String[]{"대형견", "대형견까지 가능", "25kg 이상", "25kg이상"};
        };
    }

    private String[] deniedTerms(PetSize size) {
        return switch (size) {
            case SMALL -> new String[]{"소형견 불가", "소형견불가", "소형견 금지", "소형견금지"};
            case MEDIUM -> new String[]{"중형견 불가", "중형견불가", "중형견 금지", "중형견금지"};
            case LARGE -> new String[]{"대형견 불가", "대형견불가", "대형견 금지", "대형견금지"};
        };
    }

    private enum PetSize {
        SMALL, MEDIUM, LARGE
    }

    private PlaceSearchDocument.Location location(Destination row) {
        return row.getMapY() == null || row.getMapX() == null ? null
                : new PlaceSearchDocument.Location(row.getMapY().doubleValue(), row.getMapX().doubleValue());
    }

    private PlaceSearchDocument.Location location(Double lat, Double lon) {
        return lat == null || lon == null ? null : new PlaceSearchDocument.Location(lat, lon);
    }

    private String regionCode(String koreanRegion) {
        GangwonRegion region = GangwonRegion.fromKoreanRegion(koreanRegion);
        return region == null ? null : region.name();
    }

    private String regionCodeFromTour(String code) {
        if (code == null) return null;
        return java.util.Arrays.stream(GangwonRegion.values()).filter(region -> region.tourApiSigunguCode().equals(code))
                .map(Enum::name).findFirst().orElse(null);
    }

    private String join(String... values) {
        return String.join(" ", java.util.Arrays.stream(values).filter(v -> v != null && !v.isBlank()).toList());
    }

    private String timestamp(java.time.LocalDateTime value) {
        return value == null ? null : value.toString();
    }
}
