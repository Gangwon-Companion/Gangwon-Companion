package com.gangwon.companion.domain.restaurant.service;

import com.gangwon.companion.domain.restaurant.entity.Restaurant;
import com.gangwon.companion.domain.restaurant.repository.RestaurantRepository;
import com.gangwon.companion.global.external.tourapi.TourApiClient;
import com.gangwon.companion.global.external.tourapi.dto.TourApiItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RestaurantSyncService {

    private static final int PAGE_SIZE = 100;

    private static final Map<String, String> LCLSSYSTM3_MENU_TYPE_MAP = Map.ofEntries(
            Map.entry("FD020100", "중식"),
            Map.entry("FD020200", "일식"),
            Map.entry("FD020300", "서양식"),
            Map.entry("FD020400", "기타외국식"),
            Map.entry("FD020500", "퓨전음식"),
            Map.entry("FD030100", "제과"),
            Map.entry("FD030200", "패스트푸드"),
            Map.entry("FD030300", "치킨"),
            Map.entry("FD030400", "분식"),
            Map.entry("FD030500", "기타"),
            Map.entry("FD030600", "기타")
    );

    private static final Map<String, String> LCLSSYSTM2_MENU_TYPE_MAP = Map.of(
            "FD01", "한식",
            "FD02", "외국식",
            "FD03", "간이음식",
            "FD04", "주점",
            "FD05", "카페"
    );

    private final RestaurantRepository restaurantRepository;
    private final TourApiClient tourApiClient;

    @Transactional
    public void sync() {
        log.info("음식점 데이터 동기화 시작");
        int pageNo = 1;
        int savedCount = 0;
        int updatedCount = 0;

        while (true) {
            List<TourApiItem> items = tourApiClient.fetchRestaurants(pageNo, PAGE_SIZE);
            if (items.isEmpty()) break;

            for (TourApiItem item : items) {
                if (item.getContentid() == null) continue;

                Optional<Restaurant> existing = restaurantRepository.findByExternalId(item.getContentid());
                if (existing.isPresent()) {
                    updateRestaurant(existing.get(), item);
                    updatedCount++;
                } else {
                    createRestaurant(item);
                    savedCount++;
                }
            }

            if (items.size() < PAGE_SIZE) break;
            pageNo++;
        }

        log.info("음식점 동기화 완료 - 신규: {}, 업데이트: {}", savedCount, updatedCount);
    }

    private void createRestaurant(TourApiItem item) {
        String menuType = resolveMenuType(item);
        restaurantRepository.save(Restaurant.builder()
                .externalId(item.getContentid())
                .name(item.getTitle())
                .menuType(menuType)
                .region(extractRegion(item.getAddr1()))
                .rating(0.0)
                .thumbnailUrl(item.getFirstimage())
                .address(item.getAddr1())
                .latitude(parseDouble(item.getMapy()))
                .longitude(parseDouble(item.getMapx()))
                .build());
    }

    private void updateRestaurant(Restaurant restaurant, TourApiItem item) {
        restaurant.updateFromApi(
                item.getTitle(),
                resolveMenuType(item),
                extractRegion(item.getAddr1()),
                item.getFirstimage(),
                item.getAddr1(),
                parseDouble(item.getMapy()),
                parseDouble(item.getMapx())
        );
    }

    private String resolveMenuType(TourApiItem item) {
        String lclsSystm3 = item.getLclsSystm3();
        if (lclsSystm3 != null && !lclsSystm3.isBlank()) {
            String mapped = LCLSSYSTM3_MENU_TYPE_MAP.get(lclsSystm3);
            if (mapped != null) return mapped;
        }

        String lclsSystm2 = item.getLclsSystm2();
        if (lclsSystm2 != null && !lclsSystm2.isBlank()) {
            String mapped = LCLSSYSTM2_MENU_TYPE_MAP.get(lclsSystm2);
            if (mapped != null) return mapped;
        }

        return "기타";
    }

    private String extractRegion(String addr1) {
        if (addr1 == null || addr1.isBlank()) return "기타";
        String[] parts = addr1.trim().split("\\s+");
        if (parts.length >= 2) {
            return parts[1].replaceAll("[시군구]$", "");
        }
        return "기타";
    }

    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) return 0.0;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
