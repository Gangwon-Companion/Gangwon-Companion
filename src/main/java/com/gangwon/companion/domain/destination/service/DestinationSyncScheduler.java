package com.gangwon.companion.domain.destination.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DestinationSyncScheduler {

    private final DestinationSyncService destinationSyncService;

    @Value("${destination-sync.enabled:true}")
    private boolean enabled;

    @Scheduled(
            cron = "${destination-sync.cron:0 0 2 * * *}",
            zone = "${destination-sync.zone:Asia/Seoul}"
    )
    public void syncDailyDestinations() {
        if (!enabled) {
            return;
        }

        log.info("Daily destination list sync started.");

        int koreanSavedCount = destinationSyncService.syncKoreanDestinations();
        int petSavedCount = destinationSyncService.syncPetDestinations();
        int accessibilitySavedCount = destinationSyncService.syncAccessibilityDestinations();

        log.info(
                "Daily destination list sync finished. koreanSavedCount={}, petSavedCount={}, accessibilitySavedCount={}",
                koreanSavedCount,
                petSavedCount,
                accessibilitySavedCount
        );
    }
}
