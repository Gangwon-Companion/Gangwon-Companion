package com.gangwon.companion.domain.restaurant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gangwon.companion.domain.restaurant.entity.Restaurant;
import com.gangwon.companion.domain.restaurant.entity.RestaurantPhoto;
import com.gangwon.companion.domain.restaurant.repository.RestaurantPhotoRepository;
import com.gangwon.companion.domain.restaurant.repository.RestaurantRepository;
import com.gangwon.companion.global.external.tourapi.TourApiClient;
import com.gangwon.companion.global.external.tourapi.dto.TourApiItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RestaurantSyncServiceTest {

    private final RestaurantRepository restaurantRepository = mock(RestaurantRepository.class);
    private final RestaurantPhotoRepository restaurantPhotoRepository = mock(RestaurantPhotoRepository.class);
    private final TourApiClient tourApiClient = mock(TourApiClient.class);
    private final RestaurantSyncService restaurantSyncService =
            new RestaurantSyncService(restaurantRepository, restaurantPhotoRepository, tourApiClient);
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

    @Test
    @DisplayName("enrichDetails stores restaurant intro fields")
    void enrichDetails_storesRestaurantIntroFields() throws Exception {
        Restaurant restaurant = Restaurant.builder()
                .name("테스트 음식점")
                .menuType("한식")
                .region("춘천")
                .rating(0.0)
                .thumbnailUrl("")
                .address("강원특별자치도 춘천시")
                .latitude(37.8)
                .longitude(127.7)
                .externalId("1")
                .build();
        TourApiItem intro = objectMapper.readValue("""
                {
                  "firstmenu": "닭갈비",
                  "treatmenu": "막국수 / 볶음밥",
                  "opentimefood": "10:00~21:00",
                  "restdatefood": "월요일",
                  "parkingfood": "가능",
                  "infocenterfood": "033-000-0000"
                }
                """, TourApiItem.class);
        given(restaurantRepository.findNeedingDetails(any(Pageable.class))).willReturn(List.of(restaurant));
        given(tourApiClient.fetchDetailIntro("1", "39")).willReturn(Optional.of(intro));
        given(tourApiClient.fetchDetailImages("1")).willReturn(List.of());

        restaurantSyncService.enrichDetails();

        assertThat(restaurant.getFirstMenu()).isEqualTo("닭갈비");
        assertThat(restaurant.getTreatMenu()).isEqualTo("막국수 / 볶음밥");
        assertThat(restaurant.getOpenTime()).isEqualTo("10:00~21:00");
        assertThat(restaurant.getRestDate()).isEqualTo("월요일");
        assertThat(restaurant.getParking()).isEqualTo("가능");
        assertThat(restaurant.getInfoCenter()).isEqualTo("033-000-0000");
        verify(restaurantRepository).save(restaurant);
    }

    @Test
    @DisplayName("enrichDetails stores restaurant detail images")
    void enrichDetails_storesRestaurantDetailImages() throws Exception {
        Restaurant restaurant = Restaurant.builder()
                .name("테스트 음식점")
                .menuType("한식")
                .region("춘천")
                .rating(0.0)
                .thumbnailUrl("")
                .address("강원특별자치도 춘천시")
                .latitude(37.8)
                .longitude(127.7)
                .externalId("1")
                .build();
        TourApiItem image = objectMapper.readValue("""
                {
                  "originimgurl": "https://example.com/origin.jpg",
                  "smallimageurl": "https://example.com/small.jpg",
                  "serialnum": "1"
                }
                """, TourApiItem.class);
        given(restaurantRepository.findNeedingDetails(any(Pageable.class))).willReturn(List.of(restaurant));
        given(tourApiClient.fetchDetailIntro("1", "39")).willReturn(Optional.empty());
        given(tourApiClient.fetchDetailImages("1")).willReturn(List.of(image));
        given(restaurantPhotoRepository.existsByRestaurantIdAndSerialNum(restaurant.getId(), "1")).willReturn(false);

        restaurantSyncService.enrichDetails();

        ArgumentCaptor<RestaurantPhoto> captor = ArgumentCaptor.forClass(RestaurantPhoto.class);
        verify(restaurantPhotoRepository).save(captor.capture());
        assertThat(captor.getValue().getOriginImgUrl()).isEqualTo("https://example.com/origin.jpg");
        assertThat(captor.getValue().getSmallImgUrl()).isEqualTo("https://example.com/small.jpg");
        assertThat(captor.getValue().getSerialNum()).isEqualTo("1");
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
