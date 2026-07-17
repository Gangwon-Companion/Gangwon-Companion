package com.gangwon.companion.domain.destination.controller;

import com.gangwon.companion.domain.destination.dto.ThemeDestinationListResponseDto;
import com.gangwon.companion.domain.destination.service.DestinationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@Tag(name = "테마별 여행지", description = "테마별 여행지 목록 및 필터 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/themes/{themeId}/destinations")
public class DestinationController {
    private final DestinationService destinationService;

    @Operation(summary = "테마별 여행지 목록 조회")
    @GetMapping
    public ResponseEntity<ThemeDestinationListResponseDto> getDestinationListByThemeId(
            @Parameter(description = "테마 ID") @PathVariable Long themeId,
            @Parameter(description = "반려동물 정보 보유 여부 필터") @RequestParam(defaultValue = "false") boolean pet,
            @Parameter(description = "무장애/접근성 정보 보유 여부 필터") @RequestParam(defaultValue = "false") boolean accessibility
    ) {
        ThemeDestinationListResponseDto destinationListByThemeId = destinationService.getDestinationListByThemeId(themeId, pet, accessibility);

        return ResponseEntity.ok(destinationListByThemeId);
    }
}
