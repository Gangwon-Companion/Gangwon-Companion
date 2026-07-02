package com.gangwon.companion.domain.touristcongestion.controller;

import com.gangwon.companion.domain.touristcongestion.dto.request.HotplaceSearchCriteria;
import com.gangwon.companion.domain.touristcongestion.dto.response.HotplaceDetailResponse;
import com.gangwon.companion.domain.touristcongestion.dto.response.HotplaceListResponse;
import com.gangwon.companion.domain.touristcongestion.service.HotplaceService;
import com.gangwon.companion.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "프로모션 핫플", description = "관광지 기반 핫플레이스 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/promotions")
public class HotplaceController {

    private final HotplaceService hotplaceService;

    @Operation(summary = "핫플레이스 목록 조회")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @SecurityRequirements
    @GetMapping("/hotplace")
    public ResponseEntity<HotplaceListResponse> searchHotplaces(
            @Parameter(description = "검색어(핫플레이스명, 시군구명, 지역명)") @RequestParam(required = false) String keyword,
            @Parameter(description = "시군구명") @RequestParam(required = false) String region,
            @Parameter(description = "조회 기간(today, week, month)") @RequestParam(defaultValue = "today") String period,
            @Parameter(description = "페이지 번호(0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "5") int size) {
        HotplaceSearchCriteria criteria = new HotplaceSearchCriteria(keyword, region, period, page, size);
        return ResponseEntity.ok(hotplaceService.searchHotplaces(criteria));
    }

    @Operation(summary = "핫플레이스 상세 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "대상을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirements
    @GetMapping("/hotplace/{hotplaceId}")
    public ResponseEntity<HotplaceDetailResponse> getHotplaceDetail(
            @Parameter(description = "핫플레이스 ID") @PathVariable Long hotplaceId) {
        return ResponseEntity.ok(hotplaceService.getHotplaceDetail(hotplaceId));
    }
}
