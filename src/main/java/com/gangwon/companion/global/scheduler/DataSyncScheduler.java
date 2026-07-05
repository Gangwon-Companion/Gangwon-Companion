package com.gangwon.companion.global.scheduler;

import com.gangwon.companion.domain.lodging.service.LodgingSyncService;
import com.gangwon.companion.domain.restaurant.service.RestaurantSyncService;
import com.gangwon.companion.domain.touristcongestion.service.TouristCongestionRateSyncService;
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
    private final TouristCongestionRateSyncService touristCongestionRateSyncService;

    // 매일 새벽 2시: 전체 동기화 1회 실행
    @Scheduled(cron = "0 0 2 * * *")
    public void syncAll() {
        log.info("=== 전체 데이터 동기화 시작 ===");
        try {
            restaurantSyncService.sync();
        } catch (Exception e) {
            log.error("음식점 동기화 중 오류 발생", e);
        }
        try {
            lodgingSyncService.sync();
        } catch (Exception e) {
            log.error("숙소 동기화 중 오류 발생", e);
        }
        try {
            touristCongestionRateSyncService.sync();
        } catch (Exception e) {
            log.error("관광혼잡도 동기화 중 오류 발생", e);
        }
        try {
            lodgingSyncService.enrichDetails();
        } catch (Exception e) {
            log.error("숙소 상세 보완 중 오류 발생", e);
        }
        log.info("=== 전체 데이터 동기화 종료 ===");
    }
}
