package com.gangwon.companion.domain.destination.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DestinationDetailSyncScheduler {

    private final DestinationDetailSyncService destinationDetailSyncService;

    @Value("${destination-detail-sync.enabled:true}")
    private boolean enabled;

    @Value("${destination-detail-sync.limit:50}")
    private int limit;

    @Scheduled(
            cron = "${destination-detail-sync.cron:0 0 3 * * *}",
            zone = "${destination-detail-sync.zone:Asia/Seoul}"
    )
    public void syncDailyDestinationDetails() {
        if (!enabled) {
            return;
        }

        log.info("Daily destination detail sync started. limit={}", limit);

        destinationDetailSyncService.syncKoreanDestinationDetails(limit);
        destinationDetailSyncService.syncPetDestinationDetails(limit);
        destinationDetailSyncService.syncAccessibilityDestinationDetails(limit);

        log.info("Daily destination detail sync finished.");
    }
}
