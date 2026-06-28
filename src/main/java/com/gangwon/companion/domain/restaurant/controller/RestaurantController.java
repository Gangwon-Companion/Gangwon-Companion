package com.gangwon.companion.domain.restaurant.controller;

import com.gangwon.companion.domain.restaurant.dto.request.RestaurantReviewRequest;
import com.gangwon.companion.domain.restaurant.dto.request.RestaurantSearchCriteria;
import com.gangwon.companion.domain.restaurant.dto.response.RestaurantDetailResponse;
import com.gangwon.companion.domain.restaurant.dto.response.RestaurantListResponse;
import com.gangwon.companion.domain.restaurant.dto.response.RestaurantReviewResponse;
import com.gangwon.companion.domain.restaurant.service.RestaurantService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "음식점", description = "음식점 검색 및 상세 조회 API")
@RestController
@RequestMapping("/api/v1/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;

    @Operation(summary = "음식점 목록 검색")
    @ApiResponse(responseCode = "200", description = "검색 결과 반환")
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<RestaurantListResponse> searchRestaurants(
            @Parameter(description = "검색 키워드 (음식점 이름)") @RequestParam(required = false) String keyword,
            @Parameter(description = "음식 종류") @RequestParam(required = false) String menuType,
            @Parameter(description = "지역") @RequestParam(required = false) String region,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
        RestaurantSearchCriteria criteria = new RestaurantSearchCriteria(keyword, menuType, region, page, size);
        return ResponseEntity.ok(restaurantService.searchRestaurants(criteria));
    }

    @Operation(summary = "음식점 상세 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상세 정보 반환"),
            @ApiResponse(responseCode = "404", description = "음식점을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirements
    @GetMapping("/{restaurantId}")
    public ResponseEntity<RestaurantDetailResponse> getRestaurantDetail(
            @Parameter(description = "음식점 ID") @PathVariable Long restaurantId) {
        return ResponseEntity.ok(restaurantService.getRestaurantDetail(restaurantId));
    }

    @Operation(summary = "음식점 리뷰 작성")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "리뷰 작성 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "음식점을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{restaurantId}/reviews")
    public ResponseEntity<RestaurantReviewResponse> createReview(
            @Parameter(description = "음식점 ID") @PathVariable Long restaurantId,
            @Valid @RequestBody RestaurantReviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(restaurantService.createReview(restaurantId, userDetails.getUsername(), request));
    }

    @Operation(summary = "음식점 리뷰 수정")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "리뷰 수정 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "본인 리뷰가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "리뷰를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{restaurantId}/reviews/{reviewId}")
    public ResponseEntity<RestaurantReviewResponse> updateReview(
            @Parameter(description = "음식점 ID") @PathVariable Long restaurantId,
            @Parameter(description = "리뷰 ID") @PathVariable Long reviewId,
            @Valid @RequestBody RestaurantReviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(restaurantService.updateReview(restaurantId, reviewId, userDetails.getUsername(), request));
    }

    @Operation(summary = "음식점 리뷰 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "리뷰 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "본인 리뷰가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "리뷰를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{restaurantId}/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @Parameter(description = "음식점 ID") @PathVariable Long restaurantId,
            @Parameter(description = "리뷰 ID") @PathVariable Long reviewId,
            @AuthenticationPrincipal UserDetails userDetails) {
        restaurantService.deleteReview(restaurantId, reviewId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
