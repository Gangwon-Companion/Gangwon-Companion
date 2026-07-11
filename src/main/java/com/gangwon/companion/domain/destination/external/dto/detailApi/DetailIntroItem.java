package com.gangwon.companion.domain.destination.external.dto.detailApi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DetailIntroItem {
    @JsonProperty("contentid")
    private Long contentId;

    @JsonProperty("contenttypeid")
    private Integer contentTypeId;

    @JsonProperty("usetime")
    private String useTime;

    @JsonProperty("usetimeculture")
    private String useTimeCulture;

    @JsonProperty("usetimefestival")
    private String useTimeFestival;

    @JsonProperty("usetimeleports")
    private String useTimeLeports;

    @JsonProperty("opentime")
    private String openTime;

    @JsonProperty("playtime")
    private String playTime;

    @JsonProperty("restdate")
    private String restDate;

    @JsonProperty("restdateculture")
    private String restDateCulture;

    @JsonProperty("restdateshopping")
    private String restDateShopping;

    @JsonProperty("restdateleports")
    private String restDateLeports;

    @JsonProperty("parking")
    private String parking;

    @JsonProperty("parkingculture")
    private String parkingCulture;

    @JsonProperty("parkingshopping")
    private String parkingShopping;

    @JsonProperty("parkingleports")
    private String parkingLeports;

    @JsonProperty("infocenter")
    private String infoCenter;

    @JsonProperty("infocenterculture")
    private String infoCenterCulture;

    @JsonProperty("infocentershopping")
    private String infoCenterShopping;

    @JsonProperty("infocenterleports")
    private String infoCenterLeports;

    @JsonProperty("sponsor1tel")
    private String sponsor1Tel;

    @JsonProperty("sponsor2tel")
    private String sponsor2Tel;

    public String getUsageTime() {
        return firstNotBlank(useTime, useTimeCulture, useTimeFestival, useTimeLeports, openTime, playTime);
    }

    public String getRestDate() {
        return firstNotBlank(restDate, restDateCulture, restDateShopping, restDateLeports);
    }

    public String getParking() {
        return firstNotBlank(parking, parkingCulture, parkingShopping, parkingLeports);
    }

    public String getInquiry() {
        return firstNotBlank(infoCenter, infoCenterCulture, infoCenterShopping, infoCenterLeports, sponsor1Tel, sponsor2Tel);
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
