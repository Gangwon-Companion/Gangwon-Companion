package com.gangwon.companion.domain.restaurant.controller;

import com.gangwon.companion.domain.restaurant.service.RestaurantSyncService;
import com.gangwon.companion.global.web.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "음식점 동기화", description = "TourAPI 기반 음식점 목록 및 상세 정보 동기화 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/restaurants")
public class RestaurantSyncController {

    private final RestaurantSyncService restaurantSyncService;

    @Operation(summary = "음식점 데이터 동기화")
    @PostMapping("/sync")
    public ResponseEntity<MessageResponse> sync() {
        restaurantSyncService.sync();
        return ResponseEntity.ok(new MessageResponse("음식점 데이터 동기화가 완료되었습니다."));
    }

    @Operation(summary = "음식점 상세 정보 보완")
    @PostMapping("/details/sync")
    public ResponseEntity<Map<String, Integer>> enrichRestaurantDetails() {
        int enrichedCount = restaurantSyncService.enrichDetails();
        return ResponseEntity.ok(Map.of("enrichedCount", enrichedCount));
    }
}
