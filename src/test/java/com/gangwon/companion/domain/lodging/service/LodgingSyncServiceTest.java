package com.gangwon.companion.domain.lodging.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gangwon.companion.domain.lodging.entity.Lodging;
import com.gangwon.companion.domain.lodging.entity.LodgingPhoto;
import com.gangwon.companion.domain.lodging.repository.LodgingPhotoRepository;
import com.gangwon.companion.domain.lodging.repository.LodgingRepository;
import com.gangwon.companion.global.external.tourapi.TourApiClient;
import com.gangwon.companion.global.external.tourapi.dto.TourApiItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;

class LodgingSyncServiceTest {

    private final LodgingRepository lodgingRepository = mock(LodgingRepository.class);
    private final LodgingPhotoRepository lodgingPhotoRepository = mock(LodgingPhotoRepository.class);
    private final TourApiClient tourApiClient = mock(TourApiClient.class);
    private final LodgingSyncService lodgingSyncService =
            new LodgingSyncService(lodgingRepository, lodgingPhotoRepository, tourApiClient);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("enrichDetails stores lodging common and intro fields")
    void enrichDetails_storesLodgingCommonAndIntroFields() throws Exception {
        Lodging lodging = Lodging.builder()
                .name("테스트 숙소")
                .description(null)
                .region("강릉")
                .price(0L)
                .rating(0.0)
                .thumbnailUrl("")
                .address("강원특별자치도 강릉시")
                .latitude(37.7)
                .longitude(128.9)
                .externalId("1")
                .build();
        TourApiItem common = objectMapper.readValue("""
                {
                  "overview": "바다가 보이는 숙소입니다."
                }
                """, TourApiItem.class);
        TourApiItem intro = objectMapper.readValue("""
                {
                  "roomcount": "10실",
                  "roomtype": "디럭스 / 스위트",
                  "checkintime": "15:00",
                  "checkouttime": "11:00",
                  "parkinglodging": "가능",
                  "subfacility": "수영장 / 피트니스",
                  "infocenterlodging": "033-000-0000"
                }
                """, TourApiItem.class);
        given(lodgingRepository.findNeedingDetails(any(Pageable.class))).willReturn(List.of(lodging));
        given(tourApiClient.fetchDetailCommon("1")).willReturn(Optional.of(common));
        given(tourApiClient.fetchDetailIntro("1", "32")).willReturn(Optional.of(intro));
        given(tourApiClient.fetchDetailImages("1")).willReturn(List.of());

        lodgingSyncService.enrichDetails();

        assertThat(lodging.getDescription()).isEqualTo("바다가 보이는 숙소입니다.");
        assertThat(lodging.getRoomCount()).isEqualTo("10실");
        assertThat(lodging.getRoomType()).isEqualTo("디럭스 / 스위트");
        assertThat(lodging.getCheckInTime()).isEqualTo("15:00");
        assertThat(lodging.getCheckOutTime()).isEqualTo("11:00");
        assertThat(lodging.getParking()).isEqualTo("가능");
        assertThat(lodging.getSubFacility()).isEqualTo("수영장 / 피트니스");
        assertThat(lodging.getInfoCenter()).isEqualTo("033-000-0000");
        verify(lodgingRepository).save(lodging);
    }

    @Test
    @DisplayName("enrichDetails stores lodging detail images")
    void enrichDetails_storesLodgingDetailImages() throws Exception {
        Lodging lodging = Lodging.builder()
                .name("테스트 숙소")
                .description(null)
                .region("강릉")
                .price(0L)
                .rating(0.0)
                .thumbnailUrl("")
                .address("강원특별자치도 강릉시")
                .latitude(37.7)
                .longitude(128.9)
                .externalId("1")
                .build();
        TourApiItem image = objectMapper.readValue("""
                {
                  "originimgurl": "https://example.com/origin.jpg",
                  "smallimageurl": "https://example.com/small.jpg",
                  "serialnum": "1"
                }
                """, TourApiItem.class);
        given(lodgingRepository.findNeedingDetails(any(Pageable.class))).willReturn(List.of(lodging));
        given(tourApiClient.fetchDetailCommon("1")).willReturn(Optional.empty());
        given(tourApiClient.fetchDetailIntro("1", "32")).willReturn(Optional.empty());
        given(tourApiClient.fetchDetailImages("1")).willReturn(List.of(image));
        given(lodgingPhotoRepository.existsByLodgingIdAndSerialNum(lodging.getId(), "1")).willReturn(false);

        lodgingSyncService.enrichDetails();

        ArgumentCaptor<LodgingPhoto> captor = ArgumentCaptor.forClass(LodgingPhoto.class);
        verify(lodgingPhotoRepository).save(captor.capture());
        assertThat(captor.getValue().getOriginImgUrl()).isEqualTo("https://example.com/origin.jpg");
        assertThat(captor.getValue().getSmallImgUrl()).isEqualTo("https://example.com/small.jpg");
        assertThat(captor.getValue().getSerialNum()).isEqualTo("1");
    }
}
