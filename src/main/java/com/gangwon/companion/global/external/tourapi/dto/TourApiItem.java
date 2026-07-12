package com.gangwon.companion.global.external.tourapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TourApiItem {

    // 공통 목록 필드
    private String contentid;
    private String contenttypeid;
    private String title;
    private String addr1;
    private String firstimage;
    private String mapx;
    private String mapy;
    private String lDongRegnCd;
    private String lDongSignguCd;
    private String lclsSystm1;
    private String lclsSystm2;
    private String lclsSystm3;

    // detailCommon2 응답 필드
    private String overview;

}
