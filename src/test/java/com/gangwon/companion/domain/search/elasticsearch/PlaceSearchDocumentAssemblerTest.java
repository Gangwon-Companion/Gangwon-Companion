package com.gangwon.companion.domain.search.elasticsearch;

import com.gangwon.companion.domain.destination.entity.AccessibilityInfo;
import com.gangwon.companion.domain.destination.entity.Destination;
import com.gangwon.companion.domain.destination.entity.DestinationDetail;
import com.gangwon.companion.domain.destination.entity.PetInfo;
import com.gangwon.companion.domain.destination.entity.SourceType;
import com.gangwon.companion.domain.destination.repository.AccessibilityInfoRepository;
import com.gangwon.companion.domain.destination.repository.DestinationDetailRepository;
import com.gangwon.companion.domain.destination.repository.DestinationRepository;
import com.gangwon.companion.domain.destination.repository.PetInfoRepository;
import com.gangwon.companion.domain.lodging.entity.Lodging;
import com.gangwon.companion.domain.lodging.repository.LodgingRepository;
import com.gangwon.companion.domain.restaurant.entity.Restaurant;
import com.gangwon.companion.domain.restaurant.repository.RestaurantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlaceSearchDocumentAssemblerTest {
    private final DestinationRepository destinationRepository = mock(DestinationRepository.class);
    private final DestinationDetailRepository destinationDetailRepository = mock(DestinationDetailRepository.class);
    private final PetInfoRepository petInfoRepository = mock(PetInfoRepository.class);
    private final AccessibilityInfoRepository accessibilityInfoRepository = mock(AccessibilityInfoRepository.class);
    private final RestaurantRepository restaurantRepository = mock(RestaurantRepository.class);
    private final LodgingRepository lodgingRepository = mock(LodgingRepository.class);
    private final PlaceSearchDocumentAssembler assembler = new PlaceSearchDocumentAssembler(
            destinationRepository, destinationDetailRepository, petInfoRepository,
            accessibilityInfoRepository, restaurantRepository, lodgingRepository);

    @Test
    void includesRestaurantMenuDetailsInSearchText() {
        Restaurant restaurant = Restaurant.builder()
                .externalId("R1")
                .name("속초 바다식당")
                .menuType("한식")
                .region("속초")
                .rating(0.0)
                .thumbnailUrl("")
                .address("강원특별자치도 속초시")
                .latitude(38.20)
                .longitude(128.59)
                .build();
        restaurant.updateIntro("물회", "대게 / 홍게 / 해산물", "09:00~20:00", "연중무휴", "가능", "033-000-0000");
        when(destinationRepository.findAll()).thenReturn(List.of());
        when(restaurantRepository.findAll()).thenReturn(List.of(restaurant));
        when(lodgingRepository.findAll()).thenReturn(List.of());

        List<PlaceSearchDocument> documents = assembler.loadAll();

        assertThat(documents).hasSize(1);
        assertThat(documents.get(0).searchText())
                .contains("속초 바다식당", "한식", "물회", "대게", "홍게", "해산물", "가능");
        assertThat(documents.get(0).placeSubtype()).isEqualTo("RESTAURANT");
        assertThat(documents.get(0).documentVersion()).isEqualTo(7);
    }

    @Test
    void includesLodgingRoomFacilityAndParkingInSearchText() {
        Lodging lodging = Lodging.builder()
                .externalId("L1")
                .name("강릉 가족호텔")
                .description("바다가 보이는 숙소")
                .region("강릉")
                .price(0L)
                .rating(0.0)
                .thumbnailUrl("")
                .address("강원특별자치도 강릉시")
                .latitude(37.75)
                .longitude(128.90)
                .build();
        lodging.updateIntro("20실", "패밀리룸 / 온돌", "15:00", "11:00", "가능", "바베큐장 / 수영장", "033-000-0000");
        when(destinationRepository.findAll()).thenReturn(List.of());
        when(restaurantRepository.findAll()).thenReturn(List.of());
        when(lodgingRepository.findAll()).thenReturn(List.of(lodging));

        List<PlaceSearchDocument> documents = assembler.loadAll();

        assertThat(documents).hasSize(1);
        assertThat(documents.get(0).searchText())
                .contains("강릉 가족호텔", "바다가 보이는 숙소", "패밀리룸", "온돌", "바베큐장", "수영장", "가능");
        assertThat(documents.get(0).placeSubtype()).isEqualTo("LODGING");
        assertThat(documents.get(0).documentVersion()).isEqualTo(7);
    }

    @Test
    void derivesRestaurantSubtypeForCafeBakeryAndDessert() {
        Restaurant cafe = Restaurant.builder()
                .externalId("R2")
                .name("강릉 오션뷰 카페")
                .menuType("카페")
                .region("강릉")
                .rating(0.0)
                .thumbnailUrl("")
                .address("강원특별자치도 강릉시")
                .latitude(37.75)
                .longitude(128.90)
                .build();
        Restaurant bakery = Restaurant.builder()
                .externalId("R3")
                .name("베이커리카페 클램")
                .menuType("카페")
                .region("동해")
                .rating(0.0)
                .thumbnailUrl("")
                .address("강원특별자치도 동해시")
                .latitude(37.50)
                .longitude(129.12)
                .build();
        Restaurant dessert = Restaurant.builder()
                .externalId("R4")
                .name("속초 젤라또")
                .menuType("카페")
                .region("속초")
                .rating(0.0)
                .thumbnailUrl("")
                .address("강원특별자치도 속초시")
                .latitude(38.20)
                .longitude(128.59)
                .build();
        when(destinationRepository.findAll()).thenReturn(List.of());
        when(restaurantRepository.findAll()).thenReturn(List.of(cafe, bakery, dessert));
        when(lodgingRepository.findAll()).thenReturn(List.of());

        List<PlaceSearchDocument> documents = assembler.loadAll();

        assertThat(documents).extracting(PlaceSearchDocument::placeSubtype)
                .containsExactly("CAFE", "BAKERY", "DESSERT");
    }

    @Test
    void derivesPetPolicyFromPetInfoRowWhenNormalizedColumnsAreMissing() {
        Destination destination = destination(1L, "속초 반려견 해변", "5");
        PetInfo petInfo = PetInfo.builder()
                .destination(destination)
                .contentId(100L)
                .accompanyType("일부구역 동반가능")
                .needItems("목줄 착용")
                .caution("대형견(25kg이상)까지 가능")
                .build();
        ReflectionTestUtils.setField(petInfo, "id", 10L);
        when(destinationRepository.findAll()).thenReturn(List.of(destination));
        when(petInfoRepository.findAllByDestinationIdIn(List.of(1L))).thenReturn(List.of(petInfo));
        when(accessibilityInfoRepository.findAllByDestinationIdIn(List.of(1L))).thenReturn(List.of());
        when(destinationDetailRepository.findAllByDestinationIdIn(List.of(1L))).thenReturn(List.of());
        when(restaurantRepository.findAll()).thenReturn(List.of());
        when(lodgingRepository.findAll()).thenReturn(List.of());

        PlaceSearchDocument document = assembler.loadAll().get(0);

        assertThat(document.petAllowed()).isTrue();
        assertThat(document.largePetAllowed()).isTrue();
        assertThat(document.petInfoText()).contains("일부구역 동반가능", "목줄 착용", "대형견");
        assertThat(document.evidenceFields()).contains("pet_allowed", "pet_size");
    }

    @Test
    void derivesWheelchairPolicyFromAccessibilityTextWhenNormalizedColumnIsMissing() {
        Destination destination = destination(2L, "강릉 무장애 전망대", "1");
        AccessibilityInfo accessibilityInfo = AccessibilityInfo.builder()
                .destination(destination)
                .contentId(200L)
                .entrance("출입구까지 턱이 없어 휠체어 접근 가능함")
                .parking("장애인 주차장 있음")
                .restroom("장애인 화장실 있음")
                .build();
        ReflectionTestUtils.setField(accessibilityInfo, "id", 20L);
        when(destinationRepository.findAll()).thenReturn(List.of(destination));
        when(petInfoRepository.findAllByDestinationIdIn(List.of(2L))).thenReturn(List.of());
        when(accessibilityInfoRepository.findAllByDestinationIdIn(List.of(2L))).thenReturn(List.of(accessibilityInfo));
        when(destinationDetailRepository.findAllByDestinationIdIn(List.of(2L))).thenReturn(List.of());
        when(restaurantRepository.findAll()).thenReturn(List.of());
        when(lodgingRepository.findAll()).thenReturn(List.of());

        PlaceSearchDocument document = assembler.loadAll().get(0);

        assertThat(document.wheelchairAccessible()).isTrue();
        assertThat(document.accessibilityInfoText()).contains("휠체어 접근 가능", "장애인 주차장", "장애인 화장실");
        assertThat(document.evidenceFields()).contains("wheelchair_accessible");
    }

    @Test
    void marksEstimatedOperatingHoursWhenUsageTimeIsAmbiguousAlwaysOpen() {
        Destination destination = destination(3L, "속초 상시개방 산책로", "5");
        DestinationDetail detail = DestinationDetail.builder()
                .destination(destination)
                .sourceType(SourceType.KOREAN)
                .contentId(300L)
                .contentTypeId(12)
                .usageTime("상시 개방")
                .build();
        when(destinationRepository.findAll()).thenReturn(List.of(destination));
        when(petInfoRepository.findAllByDestinationIdIn(List.of(3L))).thenReturn(List.of());
        when(accessibilityInfoRepository.findAllByDestinationIdIn(List.of(3L))).thenReturn(List.of());
        when(destinationDetailRepository.findAllByDestinationIdIn(List.of(3L))).thenReturn(List.of(detail));
        when(restaurantRepository.findAll()).thenReturn(List.of());
        when(lodgingRepository.findAll()).thenReturn(List.of());

        PlaceSearchDocument document = assembler.loadAll().get(0);

        assertThat(document.opensAt()).isEqualTo("00:00");
        assertThat(document.closesAt()).isEqualTo("22:00");
        assertThat(document.operatingHoursRaw()).isEqualTo("상시 개방");
        assertThat(document.evidenceFields()).contains("opens_at", "closes_at", "operating_hours_estimated");
    }

    private Destination destination(long id, String title, String sigunguCode) {
        Destination destination = Destination.builder()
                .primaryContentId(id)
                .primarySourceType(SourceType.KOREAN)
                .contentTypeId(12)
                .title(title)
                .addr1("강원특별자치도")
                .sigunguCode(sigunguCode)
                .mapX(BigDecimal.valueOf(128.59))
                .mapY(BigDecimal.valueOf(38.20))
                .build();
        ReflectionTestUtils.setField(destination, "id", id);
        return destination;
    }
}
