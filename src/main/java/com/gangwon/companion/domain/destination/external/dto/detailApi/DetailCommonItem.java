package com.gangwon.companion.domain.destination.external.dto.detailApi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DetailCommonItem {
    @JsonProperty("contentid")
    private Long contentId;

    @JsonProperty("contenttypeid")
    private Integer contentTypeId;

    private String overview;

    private String homepage;
}
