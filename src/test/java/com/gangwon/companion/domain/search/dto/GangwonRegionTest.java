package com.gangwon.companion.domain.search.dto;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class GangwonRegionTest {
    @Test
    void hasEighteenUniqueTourApiCodes() {
        assertThat(GangwonRegion.values()).hasSize(18);
        assertThat(Arrays.stream(GangwonRegion.values())
                .map(GangwonRegion::tourApiSigunguCode)
                .distinct()).hasSize(18);
    }

    @Test
    void mapsKoreanCityAndCountyNames() {
        assertThat(GangwonRegion.fromKoreanRegion("강릉시")).isEqualTo(GangwonRegion.GANGNEUNG);
        assertThat(GangwonRegion.fromKoreanRegion("양양군")).isEqualTo(GangwonRegion.YANGYANG);
        assertThat(GangwonRegion.fromKoreanRegion("기타")).isNull();
    }
}
