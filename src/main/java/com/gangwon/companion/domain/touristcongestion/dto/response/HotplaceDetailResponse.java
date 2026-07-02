package com.gangwon.companion.domain.touristcongestion.dto.response;

import com.gangwon.companion.domain.touristcongestion.entity.TouristCongestionRate;
import lombok.Getter;

@Getter
public class HotplaceDetailResponse {

    private final Long hotplaceId;
    private final String attractionName;
    private final String areaCode;
    private final String signguCode;
    private final String areaName;
    private final String signguName;
    private final String displayName;
    private final Double congestionRate;
    private final String baseDate;

    public HotplaceDetailResponse(TouristCongestionRate rate) {
        this.hotplaceId = rate.getId();
        this.attractionName = rate.getAttractionName();
        this.areaCode = rate.getAreaCode();
        this.signguCode = rate.getSignguCode();
        this.areaName = rate.getAreaName();
        this.signguName = rate.getSignguName();
        this.displayName = formatDisplayName(rate.getSignguName(), rate.getAttractionName());
        this.congestionRate = rate.getCongestionRate();
        this.baseDate = rate.getBaseDate();
    }

    private String formatDisplayName(String signguName, String attractionName) {
        if (signguName == null || signguName.isBlank()) {
            return attractionName;
        }
        if (attractionName == null || attractionName.isBlank()) {
            return signguName;
        }
        return signguName + " " + attractionName;
    }
}
