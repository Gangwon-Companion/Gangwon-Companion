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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HotplaceService {

    private static final int HOTPLACE_LIMIT = 5;

    private final TouristCongestionRateRepository repository;

    @Transactional(readOnly = true)
    public HotplaceListResponse searchHotplaces(HotplaceSearchCriteria criteria) {
        Page<TouristCongestionRate> result = repository.findAll(
                TouristCongestionRateSpecifications.from(criteria),
                PageRequest.of(
                        0,
                        HOTPLACE_LIMIT,
                        Sort.by(
                                Sort.Order.desc("congestionRate"),
                                Sort.Order.desc("baseDate"),
                                Sort.Order.desc("id")
                        )
                )
        );

        List<HotplaceItemResponse> items = result.getContent().stream()
                .map(HotplaceItemResponse::new)
                .toList();

        return new HotplaceListResponse(result.getTotalElements(), items);
    }

    @Transactional(readOnly = true)
    public HotplaceDetailResponse getHotplaceDetail(Long hotplaceId) {
        TouristCongestionRate rate = repository.findById(hotplaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return new HotplaceDetailResponse(rate);
    }
}
