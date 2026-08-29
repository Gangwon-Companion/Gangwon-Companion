package com.gangwon.companion.domain.destination.controller;

import com.gangwon.companion.domain.destination.dto.DestinationDetailResponseDto;
import com.gangwon.companion.domain.destination.dto.DestinationReviewRequest;
import com.gangwon.companion.domain.destination.dto.DestinationReviewResponse;
import com.gangwon.companion.domain.destination.service.DestinationDetailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "여행지 상세", description = "여행지 상세 정보 및 리뷰 API")
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

    @Operation(summary = "여행지 리뷰 작성")
    @PostMapping("/{destinationId}/reviews")
    public ResponseEntity<DestinationReviewResponse> createReview(
            @Parameter(description = "여행지 ID") @PathVariable Long destinationId,
            @Valid @RequestBody DestinationReviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(destinationDetailService.createReview(destinationId, userDetails.getUsername(), request));
    }

    @Operation(summary = "여행지 리뷰 수정")
    @PatchMapping("/{destinationId}/reviews/{reviewId}")
    public ResponseEntity<DestinationReviewResponse> updateReview(
            @Parameter(description = "여행지 ID") @PathVariable Long destinationId,
            @Parameter(description = "리뷰 ID") @PathVariable Long reviewId,
            @Valid @RequestBody DestinationReviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(destinationDetailService.updateReview(destinationId, reviewId, userDetails.getUsername(), request));
    }

    @Operation(summary = "여행지 리뷰 삭제")
    @DeleteMapping("/{destinationId}/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @Parameter(description = "여행지 ID") @PathVariable Long destinationId,
            @Parameter(description = "리뷰 ID") @PathVariable Long reviewId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        destinationDetailService.deleteReview(destinationId, reviewId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
