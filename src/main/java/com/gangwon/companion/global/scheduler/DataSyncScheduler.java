package com.gangwon.companion.global.scheduler;

import com.gangwon.companion.domain.lodging.service.LodgingSyncService;
import com.gangwon.companion.domain.restaurant.service.RestaurantSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSyncScheduler {

    private final RestaurantSyncService restaurantSyncService;
    private final LodgingSyncService lodgingSyncService;

    // 매일 새벽 2시: 목록 동기화 (API 호출 최소화)
    @Scheduled(cron = "0 0 2 * * *")
    public void syncAll() {
        log.info("=== 관광 데이터 자동 동기화 시작 ===");
        try {
            restaurantSyncService.sync();
        } catch (Exception e) {
            log.error("음식점 동기화 중 오류 발생", e);
        }
        try {
            lodgingSyncService.sync();
        } catch (Exception e) {
            log.error("숙박 동기화 중 오류 발생", e);
        }
        log.info("=== 관광 데이터 자동 동기화 완료 ===");
    }

    // 매일 새벽 3시: 숙박 상세 보완 (하루 50건, 300ms 간격으로 할당량 보호)
    @Scheduled(cron = "0 0 3 * * *")
    public void enrichLodgingDetails() {
        log.info("=== 숙박 상세 보완 시작 ===");
        try {
            lodgingSyncService.enrichDetails();
        } catch (Exception e) {
            log.error("숙박 상세 보완 중 오류 발생", e);
        }
        log.info("=== 숙박 상세 보완 완료 ===");
    }
}
