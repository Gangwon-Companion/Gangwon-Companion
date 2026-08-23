package com.gangwon.companion.domain.search.elasticsearch;

import com.gangwon.companion.domain.lodging.entity.Lodging;
import com.gangwon.companion.domain.lodging.repository.LodgingRepository;
import com.gangwon.companion.domain.restaurant.entity.Restaurant;
import com.gangwon.companion.domain.restaurant.repository.RestaurantRepository;
import com.gangwon.companion.domain.destination.entity.Destination;
import com.gangwon.companion.domain.destination.entity.PetInfo;
import com.gangwon.companion.domain.destination.entity.SourceType;
import com.gangwon.companion.domain.destination.repository.DestinationRepository;
import com.gangwon.companion.domain.destination.repository.PetInfoRepository;
import com.gangwon.companion.domain.search.dto.PlaceSearchRequest;
import com.gangwon.companion.domain.search.service.PlaceSearchEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "ELASTICSEARCH_INTEGRATION_URL", matches = ".+")
class ElasticsearchSearchIntegrationTest {
    @DynamicPropertySource
    static void elasticsearch(DynamicPropertyRegistry registry) {
        registry.add("search.engine", () -> "elasticsearch");
        registry.add("search.elasticsearch.url", () -> System.getenv("ELASTICSEARCH_INTEGRATION_URL"));
        registry.add("search.elasticsearch.alias", () -> "gangwon-places-integration");
        registry.add("search.elasticsearch.index-prefix", () -> "gangwon-places-integration-v1");
    }

    @Autowired RestaurantRepository restaurantRepository;
    @Autowired LodgingRepository lodgingRepository;
    @Autowired DestinationRepository destinationRepository;
    @Autowired PetInfoRepository petInfoRepository;
    @Autowired ElasticsearchIndexService indexService;
    @Autowired PlaceSearchEngine searchEngine;

    @BeforeEach
    void seed() {
        restaurantRepository.deleteAllInBatch();
        lodgingRepository.deleteAllInBatch();
        petInfoRepository.deleteAllInBatch();
        destinationRepository.deleteAllInBatch();
        restaurantRepository.save(Restaurant.builder().externalId("ES-R-1").name("강릉 바다 한식당")
                .menuType("한식").region("강릉").rating(0.0).thumbnailUrl("").address("강원특별자치도 강릉시")
                .latitude(37.75).longitude(128.90).build());
        lodgingRepository.save(Lodging.builder().externalId("ES-L-1").name("강릉 오션뷰 호텔")
                .description("바다가 보이는 조용한 숙소").region("강릉").price(0L).rating(0.0)
                .thumbnailUrl("").address("강원특별자치도 강릉시").latitude(37.76).longitude(128.91).build());
        Destination denied = destinationRepository.save(Destination.builder().primaryContentId(999L)
                .primarySourceType(SourceType.KOREAN).contentTypeId(12).title("반려동물 출입 금지 해변")
                .addr1("강원특별자치도 강릉시").sigunguCode("1").mapX(new BigDecimal("128.90"))
                .mapY(new BigDecimal("37.75")).build());
        PetInfo policy = PetInfo.builder().destination(denied).contentId(999L).build();
        policy.applySearchNormalization(false, false, false, false, "v1");
        petInfoRepository.save(policy);
    }

    @Test
    void reindexesRdbAggregatesAndSearchesWithBm25FiltersAndGeo() {
        var report = indexService.reindex();
        assertThat(report.sourceCount()).isEqualTo(3);
        assertThat(report.indexedCount()).isEqualTo(3);
        assertThat(report.aliasSwitched()).isTrue();

        var request = new PlaceSearchRequest(PlaceSearchRequest.Domain.LODGING, "D1_LODGING",
                List.of(PlaceSearchRequest.RegionCode.GANGNEUNG), "조용한 바다 숙소",
                new PlaceSearchRequest.HardFilters(true, PlaceSearchRequest.PetSize.SMALL, true),
                Map.of("ocean_view", 0.8), new PlaceSearchRequest.GeoConstraint(
                new PlaceSearchRequest.GeoCenter(37.75, 128.90), 5.0), 5);

        var response = searchEngine.search(request);
        assertThat(response.results()).hasSize(1);
        assertThat(response.results().get(0).name()).isEqualTo("강릉 오션뷰 호텔");
        assertThat(response.results().get(0).status().name()).isEqualTo("INSUFFICIENT_EVIDENCE");
        assertThat(response.results().get(0).missingFields())
                .containsExactly("pet_allowed", "pet_size", "wheelchair_accessible");
        assertThat(response.results().get(0).distanceKm()).isNotNull();

        var deniedRequest = new PlaceSearchRequest(PlaceSearchRequest.Domain.DESTINATION, "D1_DESTINATION",
                List.of(PlaceSearchRequest.RegionCode.GANGNEUNG), "해변",
                new PlaceSearchRequest.HardFilters(true, PlaceSearchRequest.PetSize.SMALL, null), Map.of(), null, 5);
        assertThat(searchEngine.search(deniedRequest).results()).isEmpty();
    }
}
