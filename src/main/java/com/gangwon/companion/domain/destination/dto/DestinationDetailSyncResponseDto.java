package com.gangwon.companion.domain.destination.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DestinationDetailSyncResponseDto {
    private int savedCount;
    private int processedCount;
    private String stoppedReason;
}
