package com.gangwon.companion.domain.search.elasticsearch;

import com.gangwon.companion.domain.destination.entity.AccessibilityInfo;
import com.gangwon.companion.domain.destination.entity.Destination;
import com.gangwon.companion.domain.destination.entity.PetInfo;
import com.gangwon.companion.domain.destination.repository.AccessibilityInfoRepository;
import com.gangwon.companion.domain.destination.repository.DestinationRepository;
import com.gangwon.companion.domain.destination.repository.PetInfoRepository;
import com.gangwon.companion.domain.lodging.repository.LodgingRepository;
import com.gangwon.companion.domain.restaurant.repository.RestaurantRepository;
import com.gangwon.companion.domain.search.dto.GangwonRegion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PlaceSearchDocumentAssembler {
    private final DestinationRepository destinationRepository;
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
                regionCode(row.getRegion()), join(row.getName(), row.getMenuType(), row.getAddress()),
                new PlaceSearchDocument.Location(row.getLatitude(), row.getLongitude()),
                null, null, null, null, null, "TOUR_API", List.of())).forEach(documents::add);
        lodgingRepository.findAll().stream().map(row -> new PlaceSearchDocument(
                "LODGING:" + row.getId(), "LODGING", row.getName(), row.getAddress(),
                regionCode(row.getRegion()), join(row.getName(), row.getDescription(), row.getAddress()),
                new PlaceSearchDocument.Location(row.getLatitude(), row.getLongitude()),
                null, null, null, null, null, "TOUR_API", List.of())).forEach(documents::add);
        return documents;
    }

    private List<PlaceSearchDocument> destinations() {
        List<Destination> rows = destinationRepository.findAll();
        List<Long> ids = rows.stream().map(Destination::getId).toList();
        Map<Long, PetInfo> pets = firstPetByDestination(ids);
        Map<Long, AccessibilityInfo> access = firstAccessByDestination(ids);
        return rows.stream().map(row -> {
            PetInfo pet = pets.get(row.getId());
            AccessibilityInfo accessibility = access.get(row.getId());
            List<String> evidence = new ArrayList<>();
            if (pet != null && pet.getPetAllowed() != null) evidence.add("pet_allowed");
            if (pet != null && anyPetSize(pet)) evidence.add("pet_size");
            if (accessibility != null && accessibility.getWheelchairAccessible() != null) evidence.add("wheelchair_accessible");
            String theme = row.getTheme() == null ? null : row.getTheme().getName();
            return new PlaceSearchDocument("DESTINATION:" + row.getId(), "DESTINATION", row.getTitle(),
                    join(row.getAddr1(), row.getAddr2()), regionCodeFromTour(row.getSigunguCode()),
                    join(row.getTitle(), row.getAddr1(), row.getAddr2(), theme), location(row),
                    pet == null ? null : pet.getPetAllowed(), pet == null ? null : pet.getSmallPetAllowed(),
                    pet == null ? null : pet.getMediumPetAllowed(), pet == null ? null : pet.getLargePetAllowed(),
                    accessibility == null ? null : accessibility.getWheelchairAccessible(), "TOUR_API", evidence);
        }).toList();
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

    private PlaceSearchDocument.Location location(Destination row) {
        return row.getMapY() == null || row.getMapX() == null ? null
                : new PlaceSearchDocument.Location(row.getMapY().doubleValue(), row.getMapX().doubleValue());
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
}
