package com.gangwon.companion.domain.lodging.controller;

import com.gangwon.companion.domain.lodging.service.LodgingSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "숙소 동기화", description = "TourAPI 기반 숙소 목록 및 상세 정보 동기화 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/lodgings")
public class LodgingSyncController {

    private final LodgingSyncService lodgingSyncService;

    @Operation(summary = "숙소 상세 정보 보완")
    @PostMapping("/details/sync")
    public ResponseEntity<Map<String, Integer>> enrichLodgingDetails() {
        int enrichedCount = lodgingSyncService.enrichDetails();
        return ResponseEntity.ok(Map.of("enrichedCount", enrichedCount));
    }
}
