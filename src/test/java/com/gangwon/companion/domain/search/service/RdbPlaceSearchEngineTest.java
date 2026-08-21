package com.gangwon.companion.domain.search.service;

import com.gangwon.companion.domain.destination.entity.Destination;
import com.gangwon.companion.domain.destination.entity.PetInfo;
import com.gangwon.companion.domain.destination.entity.SourceType;
import com.gangwon.companion.domain.destination.repository.AccessibilityInfoRepository;
import com.gangwon.companion.domain.destination.repository.DestinationRepository;
import com.gangwon.companion.domain.destination.repository.PetInfoRepository;
import com.gangwon.companion.domain.lodging.repository.LodgingRepository;
import com.gangwon.companion.domain.restaurant.entity.Restaurant;
import com.gangwon.companion.domain.restaurant.repository.RestaurantRepository;
import com.gangwon.companion.domain.search.dto.PlaceSearchRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = {
        "jwt.secret=gangwon_test_jwt_secret_key_2026_at_least_32_bytes_long",
        "tour-api.service-key=test-tour-api-key"
})
class RdbPlaceSearchEngineTest {
    @Autowired DestinationRepository destinationRepository;
    @Autowired PetInfoRepository petInfoRepository;
    @Autowired AccessibilityInfoRepository accessibilityInfoRepository;
    @Autowired RestaurantRepository restaurantRepository;
    @Autowired LodgingRepository lodgingRepository;
    RdbPlaceSearchEngine engine;

    @BeforeEach
    void setUp() {
        engine = new RdbPlaceSearchEngine(destinationRepository, petInfoRepository,
                accessibilityInfoRepository, restaurantRepository, lodgingRepository);
    }

    @Test
    void searchesDestinationWithNormalizedPetPolicy() {
        Destination destination = destinationRepository.save(Destination.builder()
                .primaryContentId(1L).primarySourceType(SourceType.KOREAN).contentTypeId(12)
                .title("경포 바다 산책로").addr1("강원특별자치도 강릉시").sigunguCode("1")
                .mapX(new BigDecimal("128.9076")).mapY(new BigDecimal("37.8058")).build());
        PetInfo petInfo = PetInfo.builder().destination(destination).contentId(1L).build();
        petInfo.applySearchNormalization(true, true, false, false, "v1");
        petInfoRepository.save(petInfo);

        var response = engine.search(request(PlaceSearchRequest.Domain.DESTINATION, "바다",
                new PlaceSearchRequest.HardFilters(true, PlaceSearchRequest.PetSize.SMALL, null)));

        assertThat(response.results()).hasSize(1);
        assertThat(response.results().get(0).status().name()).isEqualTo("OK");
        assertThat(response.results().get(0).placeId()).startsWith("DESTINATION:");
    }

    @Test
    void keepsRestaurantWithMissingPolicyAsInsufficientEvidence() {
        restaurantRepository.save(Restaurant.builder().externalId("R1").name("강릉 한식당")
                .menuType("한식").region("강릉").rating(0.0).thumbnailUrl("")
                .address("강원특별자치도 강릉시").latitude(37.75).longitude(128.90).build());

        var response = engine.search(request(PlaceSearchRequest.Domain.RESTAURANT, "한식",
                new PlaceSearchRequest.HardFilters(true, PlaceSearchRequest.PetSize.SMALL, true)));

        assertThat(response.results()).hasSize(1);
        assertThat(response.results().get(0).missingFields())
                .containsExactly("pet_allowed", "pet_size", "wheelchair_accessible");
        assertThat(response.results().get(0).status().name()).isEqualTo("INSUFFICIENT_EVIDENCE");
    }

    private PlaceSearchRequest request(PlaceSearchRequest.Domain domain, String query,
                                       PlaceSearchRequest.HardFilters filters) {
        return new PlaceSearchRequest(domain, "D1_TEST", List.of(PlaceSearchRequest.RegionCode.GANGNEUNG),
                query, filters, Map.of(), null, 5);
    }
}
