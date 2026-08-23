package com.gangwon.companion.domain.destination.controller;

import com.gangwon.companion.domain.destination.dto.DestinationDetailSyncResponseDto;
import com.gangwon.companion.domain.destination.service.DestinationSyncService;
import com.gangwon.companion.domain.destination.service.DestinationDetailSyncService;
import com.gangwon.companion.domain.destination.service.DestinationSearchDataBackfillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "여행지 동기화", description = "TourAPI 기반 여행지 목록 및 상세 정보 동기화 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/destinations")
public class DestinationSyncController {

    private final DestinationSyncService destinationSyncService;
    private final DestinationDetailSyncService destinationDetailSyncService;
    private final DestinationSearchDataBackfillService destinationSearchDataBackfillService;

    @Operation(summary = "국문 여행지 목록 동기화")
    @PostMapping("/sync/korean")
    public ResponseEntity<Map<String, Integer>> syncKoreanDestinations() {
        int savedCount = destinationSyncService.syncKoreanDestinations();
        return ResponseEntity.ok(Map.of("savedCount", savedCount));
    }

    @Operation(summary = "반려동물 동반 여행지 목록 동기화")
    @PostMapping("/sync/pet")
    public ResponseEntity<Map<String, Integer>> syncPetDestinations() {
        int savedCount = destinationSyncService.syncPetDestinations();
        return ResponseEntity.ok(Map.of("savedCount", savedCount));
    }

    @Operation(summary = "무장애 여행지 목록 동기화")
    @PostMapping("/sync/accessibility")
    public ResponseEntity<Map<String, Integer>> syncAccessibilityDestinations() {
        int savedCount = destinationSyncService.syncAccessibilityDestinations();
        return ResponseEntity.ok(Map.of("savedCount", savedCount));
    }

    @Operation(summary = "국문 여행지 상세 정보 동기화")
    @PostMapping("/details/sync/korean")
    public ResponseEntity<DestinationDetailSyncResponseDto> syncKoreanDestinationDetails(
            @Parameter(description = "한 번에 동기화할 최대 개수") @RequestParam(defaultValue = "50") int limit
    ) {
        DestinationDetailSyncResponseDto response = destinationDetailSyncService.syncKoreanDestinationDetails(limit);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "반려동물 여행지 상세 정보 동기화")
    @PostMapping("/details/sync/pet")
    public ResponseEntity<DestinationDetailSyncResponseDto> syncPetDestinationDetails(
            @Parameter(description = "한 번에 동기화할 최대 개수") @RequestParam(defaultValue = "50") int limit
    ) {
        DestinationDetailSyncResponseDto response = destinationDetailSyncService.syncPetDestinationDetails(limit);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "무장애 여행지 상세 정보 동기화")
    @PostMapping("/details/sync/accessibility")
    public ResponseEntity<DestinationDetailSyncResponseDto> syncAccessibilityDestinationDetails(
            @Parameter(description = "한 번에 동기화할 최대 개수") @RequestParam(defaultValue = "50") int limit
    ) {
        DestinationDetailSyncResponseDto response = destinationDetailSyncService.syncAccessibilityDestinationDetails(limit);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "여행지 검색용 파생 데이터 재정규화")
    @PostMapping("/search-data/backfill")
    public ResponseEntity<DestinationSearchDataBackfillService.BackfillResult> backfillSearchData() {
        return ResponseEntity.ok(destinationSearchDataBackfillService.backfill());
    }
}
