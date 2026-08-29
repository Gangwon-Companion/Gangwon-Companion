package com.gangwon.companion.domain.search.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OperatingHoursTest {

    @Test
    void parsesColonSeparatedRange() {
        assertThat(OperatingHours.parse("09:00~18:30"))
                .contains(new OperatingHours.Range("09:00", "18:30"));
    }

    @Test
    void parsesKoreanHourAndMinuteRange() {
        assertThat(OperatingHours.parse("오전 9시 30분부터 오후 18시까지"))
                .contains(new OperatingHours.Range("09:30", "18:00"));
    }

    @Test
    void parsesAlwaysOpenAndMidnightClosing() {
        assertThat(OperatingHours.parse("상시 개방"))
                .contains(new OperatingHours.Range("00:00", "24:00"));
        assertThat(OperatingHours.parse("평일 08:30~22:00, 주말 08:30~24:00"))
                .contains(new OperatingHours.Range("08:30", "22:00"));
    }

    @Test
    void rejectsMissingOrIncompleteRange() {
        assertThat(OperatingHours.parse(null)).isEmpty();
        assertThat(OperatingHours.parse("매주 월요일 휴무")).isEmpty();
        assertThat(OperatingHours.parse("09:00 오픈")).isEmpty();
    }
}
