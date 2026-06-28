package com.gangwon.companion.global.scheduler;

import com.gangwon.companion.domain.lodging.service.LodgingSyncService;
import com.gangwon.companion.domain.restaurant.service.RestaurantSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSyncInitializer implements ApplicationRunner {

    private final RestaurantSyncService restaurantSyncService;
    private final LodgingSyncService lodgingSyncService;

    @Override
    public void run(ApplicationArguments args) {
        // 매 시작 시 동기화 실행 (upsert 방식이라 중복 없음)
        try {
            restaurantSyncService.sync();
        } catch (Exception e) {
            log.error("음식점 동기화 실패", e);
        }
        try {
            lodgingSyncService.sync();
        } catch (Exception e) {
            log.error("숙박 동기화 실패", e);
        }
        // 상세 보완은 미완성 항목(price=0)에만, 50건씩
        try {
            lodgingSyncService.enrichDetails();
        } catch (Exception e) {
            log.error("숙박 상세 보완 실패", e);
        }
    }
}
