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
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PlaceSearchDocumentAssembler {
    private static final int DOCUMENT_VERSION = 3;

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
                regionCode(row.getRegion()), join(row.getName(), row.getMenuType(), row.getRegion(), row.getAddress()),
                location(row.getLatitude(), row.getLongitude()),
                null, null, null, null, null, null, row.getMenuType(), row.getRating(), null, null,
                null, opens(row.getOpenTime()), closes(row.getOpenTime()), row.getOpenTime(),
                timestamp(row.getCreatedAt()), DOCUMENT_VERSION, "TOUR_API", evidence(row.getOpenTime()))).forEach(documents::add);
        lodgingRepository.findAll().stream().map(row -> new PlaceSearchDocument(
                "LODGING:" + row.getId(), "LODGING", row.getName(), row.getAddress(),
                regionCode(row.getRegion()), join(row.getName(), row.getDescription(), row.getRegion(), row.getAddress()),
                location(row.getLatitude(), row.getLongitude()),
                null, null, null, null, null, null, null, row.getRating(), row.getPrice(), null,
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
                    regionCode(row.getRegion()), join(row.getName(), row.getMenuType(), row.getRegion(), row.getAddress()),
                    location(row.getLatitude(), row.getLongitude()),
                    null, null, null, null, null, null, row.getMenuType(), row.getRating(), null, null,
                    null, opens(row.getOpenTime()), closes(row.getOpenTime()), row.getOpenTime(),
                    timestamp(row.getCreatedAt()), DOCUMENT_VERSION, "TOUR_API", evidence(row.getOpenTime())));
            case "LODGING" -> lodgingRepository.findById(id).map(row -> new PlaceSearchDocument(
                    "LODGING:" + row.getId(), "LODGING", row.getName(), row.getAddress(),
                    regionCode(row.getRegion()), join(row.getName(), row.getDescription(), row.getRegion(), row.getAddress()),
                    location(row.getLatitude(), row.getLongitude()),
                    null, null, null, null, null, null, null, row.getRating(), row.getPrice(), null,
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
            if (pet != null && pet.getPetAllowed() != null) evidence.add("pet_allowed");
            if (pet != null && anyPetSize(pet)) evidence.add("pet_size");
            if (accessibility != null && accessibility.getWheelchairAccessible() != null) evidence.add("wheelchair_accessible");
            String theme = row.getTheme() == null ? null : row.getTheme().getName();
            String petInfoText = petInfoText(pet);
            String accessibilityInfoText = accessibilityInfoText(accessibility);
            String operatingHours = operatingHoursByDestination(List.of(row.getId())).get(row.getId());
            if (OperatingHours.parse(operatingHours).isPresent()) {
                evidence.add("opens_at");
                evidence.add("closes_at");
            }
            return new PlaceSearchDocument("DESTINATION:" + row.getId(), "DESTINATION", row.getTitle(),
                    join(row.getAddr1(), row.getAddr2()), regionCodeFromTour(row.getSigunguCode()),
                    join(row.getTitle(), row.getAddr1(), row.getAddr2(), theme, overviews.get(row.getId()),
                            petInfoText, accessibilityInfoText), location(row),
                    pet == null ? null : pet.getPetAllowed(), pet == null ? null : pet.getSmallPetAllowed(),
                    pet == null ? null : pet.getMediumPetAllowed(), pet == null ? null : pet.getLargePetAllowed(),
                    accessibility == null ? null : accessibility.getWheelchairAccessible(), theme, null, row.getRating(), null,
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
        if (pet != null && pet.getPetAllowed() != null) evidence.add("pet_allowed");
        if (pet != null && anyPetSize(pet)) evidence.add("pet_size");
        if (accessibility != null && accessibility.getWheelchairAccessible() != null) evidence.add("wheelchair_accessible");
        String theme = row.getTheme() == null ? null : row.getTheme().getName();
        String petInfoText = petInfoText(pet);
        String accessibilityInfoText = accessibilityInfoText(accessibility);
        String operatingHours = operatingHoursByDestination(List.of(id)).get(id);
        if (OperatingHours.parse(operatingHours).isPresent()) {
            evidence.add("opens_at");
            evidence.add("closes_at");
        }
        return new PlaceSearchDocument("DESTINATION:" + id, "DESTINATION", row.getTitle(),
                join(row.getAddr1(), row.getAddr2()), regionCodeFromTour(row.getSigunguCode()),
                join(row.getTitle(), row.getAddr1(), row.getAddr2(), theme, overview, petInfoText, accessibilityInfoText),
                location(row), pet == null ? null : pet.getPetAllowed(),
                pet == null ? null : pet.getSmallPetAllowed(), pet == null ? null : pet.getMediumPetAllowed(),
                pet == null ? null : pet.getLargePetAllowed(),
                accessibility == null ? null : accessibility.getWheelchairAccessible(), theme, null, row.getRating(), null,
                petInfoText, accessibilityInfoText, opens(operatingHours), closes(operatingHours), operatingHours,
                timestamp(row.getUpdatedAt()), DOCUMENT_VERSION, "TOUR_API", evidence);
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
        return OperatingHours.parse(raw).isPresent() ? List.of("opens_at", "closes_at") : List.of();
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

    private boolean anyPetSize(PetInfo pet) {
        return pet.getSmallPetAllowed() != null || pet.getMediumPetAllowed() != null || pet.getLargePetAllowed() != null;
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
