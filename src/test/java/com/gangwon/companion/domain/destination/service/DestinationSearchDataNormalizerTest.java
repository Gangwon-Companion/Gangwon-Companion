package com.gangwon.companion.domain.destination.service;

import com.gangwon.companion.domain.destination.entity.AccessibilityInfo;
import com.gangwon.companion.domain.destination.entity.PetInfo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DestinationSearchDataNormalizerTest {
    private final DestinationSearchDataNormalizer normalizer = new DestinationSearchDataNormalizer();

    @Test
    void normalizesSmallPetOnlyPolicy() {
        PetInfo info = petInfo("소형견에 한해 동반 가능", "이동장 필수");

        assertThat(normalizer.normalize(info)).isTrue();
        assertThat(info.getPetAllowed()).isTrue();
        assertThat(info.getSmallPetAllowed()).isTrue();
        assertThat(info.getMediumPetAllowed()).isFalse();
        assertThat(info.getLargePetAllowed()).isFalse();
    }

    @Test
    void keepsUnknownPetSizesNull() {
        PetInfo info = petInfo("반려견 동반 가능", null);

        normalizer.normalize(info);

        assertThat(info.getPetAllowed()).isTrue();
        assertThat(info.getSmallPetAllowed()).isNull();
        assertThat(info.getMediumPetAllowed()).isNull();
        assertThat(info.getLargePetAllowed()).isNull();
    }

    @Test
    void explicitPetBanOverridesOtherPetWords() {
        PetInfo info = petInfo("반려동물 동반불가", "반려견 시설 없음");

        normalizer.normalize(info);

        assertThat(info.getPetAllowed()).isFalse();
        assertThat(info.getSmallPetAllowed()).isFalse();
        assertThat(info.getMediumPetAllowed()).isFalse();
        assertThat(info.getLargePetAllowed()).isFalse();
    }

    @Test
    void normalizesWheelchairRampAsAccessible() {
        AccessibilityInfo info = accessibilityInfo("주 출입구에 경사로 있음", null);

        normalizer.normalize(info);

        assertThat(info.getWheelchairAccessible()).isTrue();
    }

    @Test
    void normalizesExplicitWheelchairBanAsInaccessible() {
        AccessibilityInfo info = accessibilityInfo("휠체어 접근 불가", "엘리베이터 있음");

        normalizer.normalize(info);

        assertThat(info.getWheelchairAccessible()).isFalse();
    }

    @Test
    void keepsAmbiguousAccessibilityNull() {
        AccessibilityInfo info = accessibilityInfo("안내 데스크 문의", null);

        normalizer.normalize(info);

        assertThat(info.getWheelchairAccessible()).isNull();
    }

    @Test
    void secondNormalizationDoesNotChangeTimestamp() {
        PetInfo info = petInfo("모든 견종 동반 가능", null);

        assertThat(normalizer.normalize(info)).isTrue();
        var firstNormalizedAt = info.getNormalizedAt();

        assertThat(normalizer.normalize(info)).isFalse();
        assertThat(info.getNormalizedAt()).isEqualTo(firstNormalizedAt);
    }

    private PetInfo petInfo(String accompanyType, String caution) {
        return PetInfo.builder()
                .contentId(1L)
                .accompanyType(accompanyType)
                .caution(caution)
                .build();
    }

    private AccessibilityInfo accessibilityInfo(String entrance, String wheelchair) {
        return AccessibilityInfo.builder()
                .contentId(1L)
                .entrance(entrance)
                .wheelchair(wheelchair)
                .build();
    }
}
