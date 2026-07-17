package com.gangwon.companion.domain.touristcongestion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.gangwon.companion.domain.touristcongestion.entity.TouristCongestionRate;
import com.gangwon.companion.domain.touristcongestion.repository.TouristCongestionRateRepository;
import com.gangwon.companion.global.external.tourapi.tatscnctrate.TatsCnctrRateClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TouristCongestionRateSyncService {

    private static final int PAGE_SIZE = 100;
    private static final String AREA_CODE = "51";

    private static final List<SignguTarget> TARGETS = List.of(
            new SignguTarget("51110"),
            new SignguTarget("51130"),
            new SignguTarget("51150"),
            new SignguTarget("51170"),
            new SignguTarget("51190"),
            new SignguTarget("51210"),
            new SignguTarget("51230"),
            new SignguTarget("51720"),
            new SignguTarget("51730"),
            new SignguTarget("51750"),
            new SignguTarget("51760"),
            new SignguTarget("51770"),
            new SignguTarget("51780"),
            new SignguTarget("51790"),
            new SignguTarget("51800"),
            new SignguTarget("51810"),
            new SignguTarget("51820")
    );

    private final TouristCongestionRateRepository repository;
    private final TatsCnctrRateClient client;

    @Transactional
    public SyncResult sync() {
        log.info("관광혼잡도 데이터 동기화 시작");
        int savedCount = 0;
        int updatedCount = 0;

        for (SignguTarget target : TARGETS) {
            int pageNo = 1;

            while (true) {
                List<JsonNode> items = client.fetchRates(AREA_CODE, target.signguCode(), pageNo, PAGE_SIZE);
                if (items.isEmpty()) {
                    break;
                }

                for (JsonNode item : items) {
                    String externalKey = buildExternalKey(AREA_CODE, target.signguCode(), item);
                    if (externalKey.isBlank()) {
                        continue;
                    }

                    Optional<TouristCongestionRate> existing = repository.findByExternalKey(externalKey);
                    if (existing.isPresent()) {
                        updateExisting(existing.get(), target.signguCode(), item);
                        updatedCount++;
                    } else {
                        createNew(target.signguCode(), item, externalKey);
                        savedCount++;
                    }
                }

                if (items.size() < PAGE_SIZE) {
                    break;
                }
                pageNo++;
            }
        }

        log.info("관광혼잡도 데이터 동기화 완료 - 신규: {}, 업데이트: {}", savedCount, updatedCount);
        return new SyncResult(savedCount, updatedCount);
    }

    private void createNew(String signguCode, JsonNode item, String externalKey) {
        repository.save(TouristCongestionRate.builder()
                .areaCode(AREA_CODE)
                .signguCode(signguCode)
                .areaName(text(item, "areaNm", "areaName"))
                .signguName(text(item, "signguNm", "signguName"))
                .attractionName(text(item, "tAtsNm", "tatsNm", "spotName", "placeName"))
                .congestionRate(parseDouble(firstNonBlank(
                        text(item, "cnctrRate"),
                        text(item, "congestionRate"),
                        text(item, "rate"),
                        text(item, "cnctrRt")
                )))
                .baseDate(firstNonBlank(
                        text(item, "baseDate"),
                        text(item, "baseYmd"),
                        text(item, "stdrYmd"),
                        text(item, "ymd")
                ))
                .rawPayload(item.toString())
                .externalKey(externalKey)
                .build());
    }

    private void updateExisting(TouristCongestionRate rate, String signguCode, JsonNode item) {
        rate.updateFromApi(
                AREA_CODE,
                signguCode,
                text(item, "areaNm", "areaName"),
                text(item, "signguNm", "signguName"),
                text(item, "tAtsNm", "tatsNm", "spotName", "placeName"),
                parseDouble(firstNonBlank(
                        text(item, "cnctrRate"),
                        text(item, "congestionRate"),
                        text(item, "rate"),
                        text(item, "cnctrRt")
                )),
                firstNonBlank(
                        text(item, "baseDate"),
                        text(item, "baseYmd"),
                        text(item, "stdrYmd"),
                        text(item, "ymd")
                ),
                item.toString()
        );
    }

    private String buildExternalKey(String areaCode, String signguCode, JsonNode item) {
        String baseDate = firstNonBlank(
                text(item, "baseDate"),
                text(item, "baseYmd"),
                text(item, "stdrYmd"),
                text(item, "ymd")
        );
        String attractionName = firstNonBlank(
                text(item, "tAtsNm"),
                text(item, "tatsNm"),
                text(item, "spotName"),
                text(item, "placeName")
        );
        String congestionRate = firstNonBlank(
                text(item, "cnctrRate"),
                text(item, "congestionRate"),
                text(item, "rate"),
                text(item, "cnctrRt")
        );

        return String.join(":",
                areaCode,
                signguCode,
                baseDate,
                attractionName,
                congestionRate
        );
    }

    private String text(JsonNode item, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode node = item.get(fieldName);
            if (node != null && !node.isNull()) {
                String value = node.asText();
                if (!value.isBlank()) {
                    return value;
                }
            }
        }
        return "";
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private record SignguTarget(String signguCode) {
    }

    public record SyncResult(int savedCount, int updatedCount) {
    }
}
