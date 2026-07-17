package com.gangwon.companion.domain.activity.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "activity.sync.enabled", havingValue = "true")
public class ActivitySyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(ActivitySyncScheduler.class);

    private final ActivitySyncService activitySyncService;

    @Scheduled(
            initialDelayString = "${activity.sync.initial-delay-ms:5000}",
            fixedDelayString = "${activity.sync.interval-ms:86400000}"
    )
    public void sync() {
        try {
            activitySyncService.syncGangwonActivities();
        } catch (RuntimeException exception) {
            log.error("TourAPI activity sync failed.", exception);
        }
    }
}
