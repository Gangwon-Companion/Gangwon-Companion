package com.gangwon.companion.domain.activity.controller;

import com.gangwon.companion.domain.activity.service.ActivitySyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "액티비티 동기화", description = "TourAPI 기반 액티비티 데이터 동기화 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/activities")
public class ActivitySyncController {

    private final ActivitySyncService activitySyncService;

    @Operation(summary = "액티비티 데이터 동기화")
    @PostMapping("/sync")
    public ResponseEntity<Map<String, Integer>> sync() {
        int savedCount = activitySyncService.syncGangwonActivities();
        return ResponseEntity.ok(Map.of("savedCount", savedCount));
    }
}
