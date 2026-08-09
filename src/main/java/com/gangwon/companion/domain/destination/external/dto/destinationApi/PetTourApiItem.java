package com.gangwon.companion.domain.destination.external.dto.destinationApi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PetTourApiItem implements DestinationApiItem {

    @JsonProperty("contentid")
    private Long contentId;

    @JsonProperty("contenttypeid")
    private Integer contentTypeId;

    private String title;
    private String addr1;
    private String addr2;

    @JsonProperty("mapx")
    private String mapX;

    @JsonProperty("mapy")
    private String mapY;

    @JsonProperty("firstimage")
    private String firstImage;

    @JsonProperty("firstimage2")
    private String firstImage2;

    private String tel;

    @JsonProperty("sigungucode")
    private String sigunguCode;

    @JsonProperty("lclsSystm1")
    private String lclsSystem1;

    @JsonProperty("lclsSystm2")
    private String lclsSystem2;

    @JsonProperty("lclsSystm3")
    private String lclsSystem3;

    public BigDecimal getMapXAsBigDecimal() {
        return toBigDecimal(mapX);
    }

    public BigDecimal getMapYAsBigDecimal() {
        return toBigDecimal(mapY);
    }

    private BigDecimal toBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return new BigDecimal(value);
    }
}
