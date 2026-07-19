package com.gangwon.companion.domain.destination.external.dto.detailApi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DetailImageItem {
    @JsonProperty("contentid")
    private Long contentId;

    @JsonProperty("originimgurl")
    private String originImgUrl;

    @JsonProperty("smallimageurl")
    private String smallImgUrl;

    @JsonProperty("serialnum")
    private String serialNum;
}
