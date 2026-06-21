package com.gangwon.companion.domain.lodging.service;

import com.gangwon.companion.domain.lodging.entity.Lodging;
import com.gangwon.companion.domain.lodging.repository.LodgingRepository;
import com.gangwon.companion.global.external.tourapi.TourApiClient;
import com.gangwon.companion.global.external.tourapi.dto.TourApiItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LodgingSyncService {

    private static final int PAGE_SIZE = 100;
    private static final int ENRICH_DELAY_MS = 300;

    private final LodgingRepository lodgingRepository;
    private final TourApiClient tourApiClient;

    @Transactional
    public void sync() {
        log.info("숙박 데이터 동기화 시작");
        int pageNo = 1;
        int savedCount = 0;
        int updatedCount = 0;

        while (true) {
            List<TourApiItem> items = tourApiClient.fetchLodgings(pageNo, PAGE_SIZE);
            if (items.isEmpty()) break;

            for (TourApiItem item : items) {
                if (item.getContentid() == null) continue;

                Optional<Lodging> existing = lodgingRepository.findByExternalId(item.getContentid());
                if (existing.isPresent()) {
                    updateLodgingBasic(existing.get(), item);
                    updatedCount++;
                } else {
                    createLodging(item);
                    savedCount++;
                }
            }

            if (items.size() < PAGE_SIZE) break;
            pageNo++;
        }

        log.info("숙박 동기화 완료 - 신규: {}, 업데이트: {}", savedCount, updatedCount);
    }

    // description이 없는 항목을 하루 50건씩 보완
    public void enrichDetails() {
        List<Lodging> targets = lodgingRepository.findTop50ByExternalIdIsNotNullAndDescriptionIsNull();
        if (targets.isEmpty()) {
            log.info("숙박 상세 보완 대상 없음");
            return;
        }

        log.info("숙박 상세 보완 시작 - 대상: {}건", targets.size());
        int enriched = 0;

        for (Lodging lodging : targets) {
            try {
                Thread.sleep(ENRICH_DELAY_MS);
                enrichSingle(lodging);
                enriched++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("숙박 상세 보완 실패: id={}, error={}", lodging.getId(), e.getMessage());
            }
        }

        log.info("숙박 상세 보완 완료 - {}건", enriched);
    }

    public void enrichSingle(Lodging lodging) {
        String externalId = lodging.getExternalId();

        String description = tourApiClient.fetchDetailCommon(externalId)
                .map(TourApiItem::getOverview)
                .filter(o -> o != null && !o.isBlank())
                .orElse("");

        lodging.updateFromApi(
                lodging.getName(),
                description,
                lodging.getRegion(),
                lodging.getPrice(),
                lodging.getThumbnailUrl(),
                lodging.getAddress(),
                lodging.getLatitude(),
                lodging.getLongitude()
        );
        lodgingRepository.save(lodging);
    }

    private void createLodging(TourApiItem item) {
        lodgingRepository.save(Lodging.builder()
                .externalId(item.getContentid())
                .name(item.getTitle())
                .description(null)
                .region(extractRegion(item.getAddr1()))
                .price(0L)
                .rating(0.0)
                .thumbnailUrl(item.getFirstimage())
                .address(item.getAddr1())
                .latitude(parseDouble(item.getMapy()))
                .longitude(parseDouble(item.getMapx()))
                .build());
    }

    private void updateLodgingBasic(Lodging lodging, TourApiItem item) {
        lodging.updateFromApi(
                item.getTitle(),
                lodging.getDescription(),
                extractRegion(item.getAddr1()),
                lodging.getPrice(),
                item.getFirstimage(),
                item.getAddr1(),
                parseDouble(item.getMapy()),
                parseDouble(item.getMapx())
        );
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
