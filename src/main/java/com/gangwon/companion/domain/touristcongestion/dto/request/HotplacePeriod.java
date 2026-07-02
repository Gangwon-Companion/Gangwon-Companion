package com.gangwon.companion.domain.touristcongestion.dto.request;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public enum HotplacePeriod {
    TODAY,
    WEEK,
    MONTH;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    public static HotplacePeriod from(String value) {
        if (value == null || value.isBlank()) {
            return TODAY;
        }

        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "today", "오늘" -> TODAY;
            case "week", "일주일", "7일" -> WEEK;
            case "month", "한달", "한달간", "1달", "30일" -> MONTH;
            default -> throw new IllegalArgumentException("Unsupported period: " + value);
        };
    }

    public LocalDate startDate() {
        return LocalDate.now(KST);
    }

    public LocalDate endDate() {
        return switch (this) {
            case TODAY -> startDate();
            case WEEK -> startDate().plusDays(7);
            case MONTH -> startDate().plusMonths(1);
        };
    }

    public String startDateValue() {
        return startDate().format(DATE_FORMAT);
    }

    public String endDateValue() {
        return endDate().format(DATE_FORMAT);
    }
}
