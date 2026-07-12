package com.gangwon.companion.domain.touristcongestion.controller;

import com.gangwon.companion.domain.touristcongestion.service.TouristCongestionRateSyncService;
import com.gangwon.companion.global.web.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "프로모션 핫플레이스 동기화", description = "관광혼잡도 데이터를 동기화하는 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/promotions")
public class TouristCongestionRateSyncController {

    private final TouristCongestionRateSyncService syncService;

    @Operation(summary = "관광혼잡도 데이터 동기화")
    @ApiResponse(responseCode = "200", description = "동기화 성공")
    @PostMapping("/hotplace/sync")
    public ResponseEntity<MessageResponse> sync() {
        TouristCongestionRateSyncService.SyncResult result = syncService.sync();
        return ResponseEntity.ok(new MessageResponse(
                "관광혼잡도 동기화가 완료되었습니다. 신규: %d, 업데이트: %d"
                        .formatted(result.savedCount(), result.updatedCount())
        ));
    }
}
