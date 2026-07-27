package com.gangwon.companion.global.scheduler;

import com.gangwon.companion.domain.activity.service.ActivitySyncService;
import com.gangwon.companion.domain.destination.dto.DestinationDetailSyncResponseDto;
import com.gangwon.companion.domain.destination.service.DestinationDetailSyncService;
import com.gangwon.companion.domain.destination.service.DestinationSyncService;
import com.gangwon.companion.domain.lodging.service.LodgingSyncService;
import com.gangwon.companion.domain.restaurant.service.RestaurantSyncService;
import com.gangwon.companion.domain.touristcongestion.service.TouristCongestionRateSyncService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSyncScheduler {

    private final ActivitySyncService activitySyncService;
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

    @Value("${activity.sync.enabled:true}")
    private boolean activitySyncEnabled;

    @PostConstruct
    public void init() {
        log.info("DataSyncScheduler bean initialized.");
    }

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
                DestinationDetailSyncResponseDto koreanResult =
                        destinationDetailSyncService.syncKoreanDestinationDetails(destinationDetailSyncLimit);
                DestinationDetailSyncResponseDto petResult =
                        destinationDetailSyncService.syncPetDestinationDetails(destinationDetailSyncLimit);
                DestinationDetailSyncResponseDto accessibilityResult =
                        destinationDetailSyncService.syncAccessibilityDestinationDetails(destinationDetailSyncLimit);
                log.info(
                        "여행지 상세 동기화 완료 - 국문 처리: {}, 국문 저장: {}, 반려동물 처리: {}, 반려동물 저장: {}, 무장애 처리: {}, 무장애 저장: {}",
                        koreanResult.getProcessedCount(),
                        koreanResult.getSavedCount(),
                        petResult.getProcessedCount(),
                        petResult.getSavedCount(),
                        accessibilityResult.getProcessedCount(),
                        accessibilityResult.getSavedCount()
                );
                if (koreanResult.getStoppedReason() != null
                        || petResult.getStoppedReason() != null
                        || accessibilityResult.getStoppedReason() != null) {
                    log.warn(
                            "여행지 상세 동기화 중단 사유 - 국문: {}, 반려동물: {}, 무장애: {}",
                            koreanResult.getStoppedReason(),
                            petResult.getStoppedReason(),
                            accessibilityResult.getStoppedReason()
                    );
                }
            } catch (Exception e) {
                log.error("여행지 상세 동기화 중 오류 발생", e);
            }
        }
        if (activitySyncEnabled) {
            try {
                int savedCount = activitySyncService.syncGangwonActivities();
                log.info("액티비티 동기화 완료 - 저장: {}", savedCount);
            } catch (Exception e) {
                log.error("액티비티 동기화 중 오류 발생", e);
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
