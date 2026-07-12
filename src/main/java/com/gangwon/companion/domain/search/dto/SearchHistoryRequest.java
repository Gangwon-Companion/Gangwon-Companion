package com.gangwon.companion.domain.search.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class SearchHistoryRequest {

    @NotBlank
    private String keyword;

    private String region;
}
