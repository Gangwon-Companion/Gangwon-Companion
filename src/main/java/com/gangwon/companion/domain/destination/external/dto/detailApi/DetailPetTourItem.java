package com.gangwon.companion.domain.destination.external.dto.detailApi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DetailPetTourItem {
    @JsonProperty("contentid")
    private Long contentId;

    @JsonProperty("acmpyTypeCd")
    private String accompanyType;

    @JsonProperty("acmpyNeedMtr")
    private String needItems;

    @JsonProperty("relaPosesFclty")
    private String possessionFacility;

    @JsonProperty("relaFrnshPrdlst")
    private String furnishedItem;

    @JsonProperty("etcAcmpyInfo")
    private String caution;

    @JsonProperty("relaAcdntRiskMtr")
    private String accidentRisk;

    public String getPetFacilities() {
        return firstNotBlank(possessionFacility, furnishedItem);
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
