package com.gangwon.companion.domain.destination.external.dto.detailApi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DetailWithTourItem {
    @JsonProperty("contentid")
    private Long contentId;

    private String parking;

    private String route;

    @JsonProperty("publictransport")
    private String entrance;

    private String elevator;

    private String restroom;

    private String wheelchair;

    @JsonProperty("braileblock")
    private String braileBlock;

    @JsonProperty("helpdog")
    private String helpDog;

    @JsonProperty("guidehuman")
    private String guideHuman;
}
