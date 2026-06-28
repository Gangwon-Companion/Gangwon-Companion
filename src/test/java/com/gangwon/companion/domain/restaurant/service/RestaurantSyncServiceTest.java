package com.gangwon.companion.domain.restaurant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gangwon.companion.domain.restaurant.entity.Restaurant;
import com.gangwon.companion.domain.restaurant.repository.RestaurantRepository;
import com.gangwon.companion.global.external.tourapi.TourApiClient;
import com.gangwon.companion.global.external.tourapi.dto.TourApiItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RestaurantSyncServiceTest {

    private final RestaurantRepository restaurantRepository = mock(RestaurantRepository.class);
    private final TourApiClient tourApiClient = mock(TourApiClient.class);
    private final RestaurantSyncService restaurantSyncService =
            new RestaurantSyncService(restaurantRepository, tourApiClient);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @ParameterizedTest
    @CsvSource({
            "FD01,FD010100,한식",
            "FD02,FD020100,중식",
            "FD02,FD020200,일식",
            "FD02,FD020300,서양식",
            "FD03,FD030400,분식",
            "FD05,FD050100,카페",
            "FD04,FD049999,주점",
            "'', '', 기타"
    })
    @DisplayName("Tour API 분류 코드를 음식 종류로 저장한다")
    void sync_mapsTourApiClassificationToMenuType(
            String lclsSystm2, String lclsSystm3, String expectedMenuType
    ) throws Exception {
        TourApiItem item = item(lclsSystm2, lclsSystm3);
        given(tourApiClient.fetchRestaurants(1, 100)).willReturn(List.of(item));
        given(restaurantRepository.findByExternalId("1")).willReturn(Optional.empty());

        restaurantSyncService.sync();

        ArgumentCaptor<Restaurant> captor = ArgumentCaptor.forClass(Restaurant.class);
        verify(restaurantRepository).save(captor.capture());
        assertThat(captor.getValue().getMenuType()).isEqualTo(expectedMenuType);
    }

    private TourApiItem item(String lclsSystm2, String lclsSystm3) throws Exception {
        return objectMapper.readValue("""
                {
                  "contentid": "1",
                  "title": "테스트 음식점",
                  "addr1": "강원특별자치도 춘천시 중앙로",
                  "lclsSystm2": "%s",
                  "lclsSystm3": "%s"
                }
                """.formatted(lclsSystm2, lclsSystm3), TourApiItem.class);
    }
}
