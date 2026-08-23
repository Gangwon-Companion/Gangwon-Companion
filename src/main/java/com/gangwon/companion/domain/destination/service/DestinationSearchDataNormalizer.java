package com.gangwon.companion.domain.destination.service;

import com.gangwon.companion.domain.destination.entity.AccessibilityInfo;
import com.gangwon.companion.domain.destination.entity.PetInfo;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class DestinationSearchDataNormalizer {
    public static final String VERSION = "v1";

    public boolean normalize(PetInfo info) {
        String text = normalizedText(
                info.getAccompanyType(), info.getNeedItems(), info.getPetFacilities(), info.getCaution());
        Boolean allowed = petAllowed(text);
        SizePolicy sizes = sizePolicy(text, allowed);
        return info.applySearchNormalization(
                allowed, sizes.small(), sizes.medium(), sizes.large(), VERSION);
    }

    public boolean normalize(AccessibilityInfo info) {
        String mobilityText = normalizedText(
                info.getRoute(), info.getEntrance(), info.getWheelchair(), info.getElevator());
        return info.applySearchNormalization(wheelchairAccessible(mobilityText), VERSION);
    }

    private Boolean petAllowed(String text) {
        if (text.isBlank()) {
            return null;
        }
        if (containsAny(text,
                "반려동물 동반 불가", "반려견 동반 불가", "반려동물 입장 불가", "반려견 입장 불가",
                "반려동물 출입 불가", "반려견 출입 불가", "반려동물 금지", "반려견 금지")) {
            return false;
        }
        if (containsAny(text,
                "동반 가능", "입장 가능", "출입 가능", "반려동물", "반려견", "애완동물", "애견")) {
            return true;
        }
        return null;
    }

    private SizePolicy sizePolicy(String text, Boolean petAllowed) {
        if (Boolean.FALSE.equals(petAllowed)) {
            return new SizePolicy(false, false, false);
        }
        if (containsAny(text, "크기 제한 없음", "견종 제한 없음", "전 견종", "모든 견종")) {
            return new SizePolicy(true, true, true);
        }
        if (containsAny(text, "소형견만", "소형견에 한", "소형견 한정")) {
            return new SizePolicy(true, false, false);
        }
        if (containsAny(text, "중소형견만", "중·소형견만", "중소형견에 한")) {
            return new SizePolicy(true, true, false);
        }
        Boolean small = containsAny(text, "소형견") ? true : null;
        Boolean medium = containsAny(text, "중형견") ? true : null;
        Boolean large = containsAny(text, "대형견") ? true : null;
        return new SizePolicy(small, medium, large);
    }

    private Boolean wheelchairAccessible(String text) {
        if (text.isBlank()) {
            return null;
        }
        if (containsAny(text,
                "휠체어 접근 불가", "휠체어 출입 불가", "휠체어 이용 불가", "경사로 없음",
                "계단만 이용", "진입 불가")) {
            return false;
        }
        if (containsAny(text,
                "휠체어 접근 가능", "휠체어 출입 가능", "휠체어 이용 가능", "경사로",
                "단차 없음", "무단차", "장애인용 엘리베이터")) {
            return true;
        }
        return null;
    }

    private String normalizedText(String... values) {
        return String.join(" ", java.util.Arrays.stream(values)
                        .filter(value -> value != null && !value.isBlank())
                        .map(value -> value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim())
                        .toList())
                .trim();
    }

    private boolean containsAny(String text, String... phrases) {
        String compactText = text.replace(" ", "");
        for (String phrase : phrases) {
            if (text.contains(phrase) || compactText.contains(phrase.replace(" ", ""))) {
                return true;
            }
        }
        return false;
    }

    private record SizePolicy(Boolean small, Boolean medium, Boolean large) {
    }
}
