package com.gangwon.companion.global.scheduler;

import com.gangwon.companion.domain.destination.service.DestinationDetailSyncService;
import com.gangwon.companion.domain.destination.service.DestinationSyncService;
import com.gangwon.companion.domain.lodging.service.LodgingSyncService;
import com.gangwon.companion.domain.restaurant.service.RestaurantSyncService;
import com.gangwon.companion.domain.touristcongestion.service.TouristCongestionRateSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSyncScheduler {

    private final DestinationSyncService destinationSyncService;
    private final DestinationDetailSyncService destinationDetailSyncService;
    private final RestaurantSyncService restaurantSyncService;
    private final LodgingSyncService lodgingSyncService;
    private final TouristCongestionRateSyncService touristCongestionRateSyncService;

    @Value("${destination-sync.enabled:true}")
    private boolean destinationSyncEnabled;

    @Value("${destination-detail-sync.enabled:true}")
    private boolean destinationDetailSyncEnabled;

    @Value("${destination-detail-sync.limit:50}")
    private int destinationDetailSyncLimit;

    // 매일 새벽 2시: 전체 데이터 동기화 1회 실행
    @Scheduled(
            cron = "${destination-sync.cron:0 0 2 * * *}",
            zone = "${destination-sync.zone:Asia/Seoul}"
    )
    public void syncAll() {
        log.info("=== 전체 데이터 동기화 시작 ===");
        if (destinationSyncEnabled) {
            try {
                int koreanSavedCount = destinationSyncService.syncKoreanDestinations();
                int petSavedCount = destinationSyncService.syncPetDestinations();
                int accessibilitySavedCount = destinationSyncService.syncAccessibilityDestinations();
                log.info(
                        "여행지 목록 동기화 완료 - 국문: {}, 반려동물: {}, 무장애: {}",
                        koreanSavedCount,
                        petSavedCount,
                        accessibilitySavedCount
                );
            } catch (Exception e) {
                log.error("여행지 목록 동기화 중 오류 발생", e);
            }
        }
        if (destinationDetailSyncEnabled) {
            try {
                destinationDetailSyncService.syncKoreanDestinationDetails(destinationDetailSyncLimit);
                destinationDetailSyncService.syncPetDestinationDetails(destinationDetailSyncLimit);
                destinationDetailSyncService.syncAccessibilityDestinationDetails(destinationDetailSyncLimit);
            } catch (Exception e) {
                log.error("여행지 상세 동기화 중 오류 발생", e);
            }
        }
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
