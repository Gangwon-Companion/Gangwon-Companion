package com.gangwon.companion.domain.destination.service;

import com.gangwon.companion.domain.destination.dto.DestinationDetailResponseDto;
import com.gangwon.companion.domain.destination.entity.AccessibilityInfo;
import com.gangwon.companion.domain.destination.entity.Destination;
import com.gangwon.companion.domain.destination.entity.DestinationDetail;
import com.gangwon.companion.domain.destination.entity.DestinationImage;
import com.gangwon.companion.domain.destination.entity.PetInfo;
import com.gangwon.companion.domain.destination.entity.SourceType;
import com.gangwon.companion.domain.destination.repository.AccessibilityInfoRepository;
import com.gangwon.companion.domain.destination.repository.DestinationDetailRepository;
import com.gangwon.companion.domain.destination.repository.DestinationImageRepository;
import com.gangwon.companion.domain.destination.repository.DestinationRepository;
import com.gangwon.companion.domain.destination.repository.PetInfoRepository;
import com.gangwon.companion.domain.theme.entity.Theme;
import com.gangwon.companion.domain.theme.repository.ThemeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class DestinationDetailServiceTest {

    @Autowired
    private DestinationDetailService destinationDetailService;

    @Autowired
    private DestinationRepository destinationRepository;

    @Autowired
    private DestinationDetailRepository destinationDetailRepository;

    @Autowired
    private DestinationImageRepository destinationImageRepository;

    @Autowired
    private PetInfoRepository petInfoRepository;

    @Autowired
    private AccessibilityInfoRepository accessibilityInfoRepository;

    @Autowired
    private ThemeRepository themeRepository;

    @Test
    void getDestinationDetailByDestinationIdReturnsKoreanDetailByDefault() {
        Theme theme = themeRepository.save(new Theme("TEST_DETAIL_KOREAN", "Test Detail Korean", 996));
        Destination destination = saveDestination(theme, "Korean Detail Destination", 4001L);
        saveDetail(destination, SourceType.KOREAN, 5001L, "Korean overview");
        saveImage(destination, SourceType.KOREAN, 5001L, "korean-origin-1.jpg", "korean-small-1.jpg", "1");
        saveImage(destination, SourceType.KOREAN, 5001L, "korean-origin-2.jpg", "korean-small-2.jpg", "2");

        DestinationDetailResponseDto result =
                destinationDetailService.getDestinationDetailByDestinationId(destination.getId(), false, false);

        assertThat(result.getDestinationId()).isEqualTo(destination.getId());
        assertThat(result.getOverview()).isEqualTo("Korean overview");
        assertThat(result.getHomepage()).isEqualTo("https://example.com/KOREAN");
        assertThat(result.getPetInfo()).isNull();
        assertThat(result.getAccessibilityInfo()).isNull();
        assertThat(result.getDestinationImageList()).hasSize(2);
        assertThat(result.getDestinationImageList())
                .extracting(image -> image.getOriginImgUrl())
                .containsExactlyInAnyOrder("korean-origin-1.jpg", "korean-origin-2.jpg");
    }

    @Test
    void getDestinationDetailByDestinationIdReturnsPetDetailWhenPetFilterIsTrue() {
        Theme theme = themeRepository.save(new Theme("TEST_DETAIL_PET", "Test Detail Pet", 995));
        Destination destination = saveDestination(theme, "Pet Detail Destination", 4002L);
        saveDetail(destination, SourceType.KOREAN, 5002L, "Korean overview");
        saveDetail(destination, SourceType.PET, 6002L, "Pet overview");
        saveImage(destination, SourceType.PET, 6002L, "pet-origin.jpg", "pet-small.jpg", "1");
        savePetInfo(destination, 6002L);

        DestinationDetailResponseDto result =
                destinationDetailService.getDestinationDetailByDestinationId(destination.getId(), true, false);

        assertThat(result.getDestinationId()).isEqualTo(destination.getId());
        assertThat(result.getOverview()).isEqualTo("Pet overview");
        assertThat(result.getPetInfo()).isNotNull();
        assertThat(result.getPetInfo().getAccompanyType()).isEqualTo("All pets allowed");
        assertThat(result.getAccessibilityInfo()).isNull();
        assertThat(result.getDestinationImageList()).hasSize(1);
        assertThat(result.getDestinationImageList().get(0).getOriginImgUrl()).isEqualTo("pet-origin.jpg");
    }

    @Test
    void getDestinationDetailByDestinationIdReturnsAccessibilityDetailWhenAccessibilityFilterIsTrue() {
        Theme theme = themeRepository.save(new Theme("TEST_DETAIL_ACCESS", "Test Detail Access", 994));
        Destination destination = saveDestination(theme, "Accessibility Detail Destination", 4003L);
        saveDetail(destination, SourceType.KOREAN, 5003L, "Korean overview");
        saveDetail(destination, SourceType.ACCESSIBILITY, 7003L, "Accessibility overview");
        saveImage(destination, SourceType.ACCESSIBILITY, 7003L, "access-origin.jpg", "access-small.jpg", "1");
        saveAccessibilityInfo(destination, 7003L);

        DestinationDetailResponseDto result =
                destinationDetailService.getDestinationDetailByDestinationId(destination.getId(), false, true);

        assertThat(result.getDestinationId()).isEqualTo(destination.getId());
        assertThat(result.getOverview()).isEqualTo("Accessibility overview");
        assertThat(result.getPetInfo()).isNull();
        assertThat(result.getAccessibilityInfo()).isNotNull();
        assertThat(result.getAccessibilityInfo().getEntrance()).isEqualTo("Wheelchair accessible entrance");
        assertThat(result.getDestinationImageList()).hasSize(1);
        assertThat(result.getDestinationImageList().get(0).getSmallImgUrl()).isEqualTo("access-small.jpg");
    }

    @Test
    void getDestinationDetailByDestinationIdReturnsBothAdditionalInfosWhenBothFiltersAreTrue() {
        Theme theme = themeRepository.save(new Theme("TEST_DETAIL_BOTH", "Test Detail Both", 993));
        Destination destination = saveDestination(theme, "Both Filter Detail Destination", 4004L);
        saveDetail(destination, SourceType.KOREAN, 5004L, "Korean overview");
        saveDetail(destination, SourceType.PET, 6004L, "Pet overview");
        saveDetail(destination, SourceType.ACCESSIBILITY, 7004L, "Accessibility overview");
        saveImage(destination, SourceType.PET, 6004L, "pet-both-origin.jpg", "pet-both-small.jpg", "1");
        savePetInfo(destination, 6004L);
        saveAccessibilityInfo(destination, 7004L);

        DestinationDetailResponseDto result =
                destinationDetailService.getDestinationDetailByDestinationId(destination.getId(), true, true);

        assertThat(result.getDestinationId()).isEqualTo(destination.getId());
        assertThat(result.getOverview()).isEqualTo("Pet overview");
        assertThat(result.getPetInfo()).isNotNull();
        assertThat(result.getPetInfo().getNeedItems()).isEqualTo("Leash required");
        assertThat(result.getAccessibilityInfo()).isNotNull();
        assertThat(result.getAccessibilityInfo().getRestroom()).isEqualTo("Accessible restroom");
        assertThat(result.getDestinationImageList()).hasSize(1);
        assertThat(result.getDestinationImageList().get(0).getOriginImgUrl()).isEqualTo("pet-both-origin.jpg");
    }

    private Destination saveDestination(Theme theme, String title, Long contentId) {
        return destinationRepository.save(Destination.builder()
                .theme(theme)
                .primaryContentId(contentId)
                .primarySourceType(SourceType.KOREAN)
                .contentTypeId(12)
                .title(title)
                .addr1("Gangwon Test Address")
                .firstImage("https://example.com/test.jpg")
                .tel("033-000-0000")
                .build());
    }

    private void saveDetail(Destination destination, SourceType sourceType, Long contentId, String overview) {
        destinationDetailRepository.save(DestinationDetail.builder()
                .destination(destination)
                .sourceType(sourceType)
                .contentId(contentId)
                .contentTypeId(12)
                .overview(overview)
                .homepage("https://example.com/" + sourceType)
                .usageTime("09:00-18:00")
                .restDate("Monday")
                .parking("Available")
                .inquiry("033-000-0000")
                .rawCommonJson("{}")
                .rawIntroJson("{}")
                .build());
    }

    private void saveImage(
            Destination destination,
            SourceType sourceType,
            Long contentId,
            String originImgUrl,
            String smallImgUrl,
            String serialNum
    ) {
        destinationImageRepository.save(DestinationImage.builder()
                .destination(destination)
                .sourceType(sourceType)
                .contentId(contentId)
                .originImgUrl(originImgUrl)
                .smallImgUrl(smallImgUrl)
                .serialNum(serialNum)
                .build());
    }

    private void savePetInfo(Destination destination, Long contentId) {
        petInfoRepository.save(PetInfo.builder()
                .destination(destination)
                .contentId(contentId)
                .accompanyType("All pets allowed")
                .needItems("Leash required")
                .petFacilities("Pet rest area")
                .caution("Clean up after pets")
                .accidentRisk("Low risk")
                .rawPetJson("{}")
                .build());
    }

    private void saveAccessibilityInfo(Destination destination, Long contentId) {
        accessibilityInfoRepository.save(AccessibilityInfo.builder()
                .destination(destination)
                .contentId(contentId)
                .parking("Accessible parking")
                .route("Flat route")
                .entrance("Wheelchair accessible entrance")
                .elevator("Elevator available")
                .restroom("Accessible restroom")
                .wheelchair("Wheelchair rental")
                .braileBlock("Braille blocks")
                .helpDog("Guide dogs allowed")
                .guideHuman("Guide available")
                .rawAccessibilityJson("{}")
                .build());
    }
}
