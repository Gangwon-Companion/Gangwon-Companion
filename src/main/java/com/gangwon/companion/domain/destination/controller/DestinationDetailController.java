package com.gangwon.companion.domain.destination.controller;

import com.gangwon.companion.domain.destination.dto.DestinationDetailResponseDto;
import com.gangwon.companion.domain.destination.service.DestinationDetailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "여행지 상세", description = "여행지 상세 정보 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/destinations")
public class DestinationDetailController {
    private final DestinationDetailService destinationDetailService;

    @Operation(summary = "여행지 상세 조회")
    @GetMapping("/{destinationId}/detail")
    public ResponseEntity<DestinationDetailResponseDto> getDestinationDetail(
            @Parameter(description = "여행지 ID") @PathVariable Long destinationId,
            @Parameter(description = "반려동물 상세 정보 포함 여부") @RequestParam(defaultValue = "false") boolean pet,
            @Parameter(description = "무장애/접근성 상세 정보 포함 여부") @RequestParam(defaultValue = "false") boolean accessibility
    ) {
        DestinationDetailResponseDto destinationDetail = destinationDetailService.getDestinationDetailByDestinationId(destinationId, pet, accessibility);

        return ResponseEntity.ok(destinationDetail);
    }

}
