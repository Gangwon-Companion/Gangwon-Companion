package com.gangwon.companion.domain.lodging.controller;

import com.gangwon.companion.domain.lodging.dto.LodgingDetailResponse;
import com.gangwon.companion.domain.lodging.dto.LodgingListResponse;
import com.gangwon.companion.domain.lodging.dto.LodgingReviewRequest;
import com.gangwon.companion.domain.lodging.dto.LodgingReviewResponse;
import com.gangwon.companion.domain.lodging.dto.LodgingSearchCriteria;
import com.gangwon.companion.domain.lodging.service.LodgingService;
import com.gangwon.companion.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "숙소", description = "숙소 검색 및 상세 조회 API")
@RestController
@RequestMapping("/api/lodgings")
@RequiredArgsConstructor
public class LodgingController {

    private final LodgingService lodgingService;

    @Operation(summary = "숙소 목록 검색")
    @ApiResponse(responseCode = "200", description = "검색 결과 반환")
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<LodgingListResponse> searchLodgings(
            @Parameter(description = "검색 키워드 (숙소 이름)") @RequestParam(required = false) String keyword,
            @Parameter(description = "지역") @RequestParam(required = false) String region,
            @Parameter(description = "최소 가격") @RequestParam(required = false) Long minPrice,
            @Parameter(description = "최대 가격") @RequestParam(required = false) Long maxPrice,
            @Parameter(description = "최소 평점") @RequestParam(required = false) Double rating,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
        LodgingSearchCriteria criteria = new LodgingSearchCriteria(keyword, region, minPrice, maxPrice, rating, page, size);
        return ResponseEntity.ok(lodgingService.searchLodgings(criteria));
    }

    @Operation(summary = "숙소 상세 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상세 정보 반환"),
            @ApiResponse(responseCode = "404", description = "숙소를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirements
    @GetMapping("/{lodgingId}")
    public ResponseEntity<LodgingDetailResponse> getLodgingDetail(
            @Parameter(description = "숙소 ID") @PathVariable Long lodgingId) {
        return ResponseEntity.ok(lodgingService.getLodgingDetail(lodgingId));
    }

    @Operation(summary = "숙소 리뷰 작성")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "리뷰 작성 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "숙소를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{lodgingId}/reviews")
    public ResponseEntity<LodgingReviewResponse> createReview(
            @Parameter(description = "숙소 ID") @PathVariable Long lodgingId,
            @Valid @RequestBody LodgingReviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lodgingService.createReview(lodgingId, userDetails.getUsername(), request));
    }

    @Operation(summary = "숙소 리뷰 수정")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "리뷰 수정 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "본인 리뷰가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "리뷰를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{lodgingId}/reviews/{reviewId}")
    public ResponseEntity<LodgingReviewResponse> updateReview(
            @Parameter(description = "숙소 ID") @PathVariable Long lodgingId,
            @Parameter(description = "리뷰 ID") @PathVariable Long reviewId,
            @Valid @RequestBody LodgingReviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(lodgingService.updateReview(lodgingId, reviewId, userDetails.getUsername(), request));
    }

    @Operation(summary = "숙소 리뷰 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "리뷰 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "본인 리뷰가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "리뷰를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{lodgingId}/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @Parameter(description = "숙소 ID") @PathVariable Long lodgingId,
            @Parameter(description = "리뷰 ID") @PathVariable Long reviewId,
            @AuthenticationPrincipal UserDetails userDetails) {
        lodgingService.deleteReview(lodgingId, reviewId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
