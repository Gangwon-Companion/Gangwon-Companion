package com.gangwon.companion.domain.search.dto;

import java.util.Arrays;

public enum GangwonRegion {
    CHUNCHEON("춘천", "13"),
    WONJU("원주", "9"),
    GANGNEUNG("강릉", "1"),
    DONGHAE("동해", "3"),
    TAEBAEK("태백", "14"),
    SOKCHO("속초", "5"),
    SAMCHEOK("삼척", "4"),
    HONGCHEON("홍천", "16"),
    HOENGSEONG("횡성", "18"),
    YEONGWOL("영월", "8"),
    PYEONGCHANG("평창", "15"),
    JEONGSEON("정선", "11"),
    CHEORWON("철원", "12"),
    HWACHEON("화천", "17"),
    YANGGU("양구", "6"),
    INJE("인제", "10"),
    GOSEONG("고성", "2"),
    YANGYANG("양양", "7");

    private final String koreanName;
    private final String tourApiSigunguCode;

    GangwonRegion(String koreanName, String tourApiSigunguCode) {
        this.koreanName = koreanName;
        this.tourApiSigunguCode = tourApiSigunguCode;
    }

    public String koreanName() {
        return koreanName;
    }

    public String tourApiSigunguCode() {
        return tourApiSigunguCode;
    }

    public static GangwonRegion fromKoreanRegion(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("[시군구]$", "");
        return Arrays.stream(values())
                .filter(region -> region.koreanName.equals(normalized))
                .findFirst()
                .orElse(null);
    }
}
