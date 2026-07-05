package com.gangwon.companion.domain.touristcongestion.service;

import com.gangwon.companion.domain.touristcongestion.dto.request.HotplaceSearchCriteria;
import com.gangwon.companion.domain.touristcongestion.dto.response.HotplaceDetailResponse;
import com.gangwon.companion.domain.touristcongestion.dto.response.HotplaceItemResponse;
import com.gangwon.companion.domain.touristcongestion.dto.response.HotplaceListResponse;
import com.gangwon.companion.domain.touristcongestion.entity.TouristCongestionRate;
import com.gangwon.companion.domain.touristcongestion.repository.TouristCongestionRateRepository;
import com.gangwon.companion.domain.touristcongestion.repository.TouristCongestionRateSpecifications;
import com.gangwon.companion.global.exception.BusinessException;
import com.gangwon.companion.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HotplaceService {

    private static final int HOTPLACE_LIMIT = 5;

    private static final Comparator<TouristCongestionRate> PEAK_ORDER = Comparator
            .comparing(TouristCongestionRate::getCongestionRate, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(TouristCongestionRate::getBaseDate, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(TouristCongestionRate::getId, Comparator.naturalOrder());

    private final TouristCongestionRateRepository repository;

    @Transactional(readOnly = true)
    public HotplaceListResponse searchHotplaces(HotplaceSearchCriteria criteria) {
        List<TouristCongestionRate> rows = repository.findAll(TouristCongestionRateSpecifications.from(criteria));

        // 같은 장소가 기간 내 여러 날 최고 혼잡도를 기록할 수 있으므로, 장소별 최고 기록 1건만 남긴다.
        List<TouristCongestionRate> peakPerPlace = rows.stream()
                .collect(Collectors.groupingBy(this::placeKey, Collectors.maxBy(PEAK_ORDER)))
                .values().stream()
                .flatMap(Optional::stream)
                .toList();

        List<TouristCongestionRate> topPlaces = peakPerPlace.stream()
                .sorted(PEAK_ORDER.reversed())
                .limit(HOTPLACE_LIMIT)
                .toList();

        List<HotplaceItemResponse> items = topPlaces.stream()
                .map(HotplaceItemResponse::new)
                .toList();

        return new HotplaceListResponse(peakPerPlace.size(), items);
    }

    private String placeKey(TouristCongestionRate rate) {
        return rate.getSignguCode() + ":" + rate.getAttractionName();
    }

    @Transactional(readOnly = true)
    public HotplaceDetailResponse getHotplaceDetail(Long hotplaceId) {
        TouristCongestionRate rate = repository.findById(hotplaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return new HotplaceDetailResponse(rate);
    }
}
