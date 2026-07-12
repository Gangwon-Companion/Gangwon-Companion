package com.gangwon.companion.domain.touristcongestion.dto.request;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HotplacePeriodTest {

    @Test
    void from_maps_expected_aliases() {
        assertThat(HotplacePeriod.from("today")).isEqualTo(HotplacePeriod.TODAY);
        assertThat(HotplacePeriod.from("오늘")).isEqualTo(HotplacePeriod.TODAY);
        assertThat(HotplacePeriod.from("week")).isEqualTo(HotplacePeriod.WEEK);
        assertThat(HotplacePeriod.from("일주일")).isEqualTo(HotplacePeriod.WEEK);
        assertThat(HotplacePeriod.from("month")).isEqualTo(HotplacePeriod.MONTH);
        assertThat(HotplacePeriod.from("한달")).isEqualTo(HotplacePeriod.MONTH);
    }

    @Test
    void from_rejects_unknown_period() {
        assertThatThrownBy(() -> HotplacePeriod.from("quarter"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
