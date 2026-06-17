package com.gangwon.companion.domain.lodging.service;

import com.gangwon.companion.domain.lodging.dto.LodgingDetailResponse;
import com.gangwon.companion.domain.lodging.dto.LodgingItemResponse;
import com.gangwon.companion.domain.lodging.dto.LodgingListResponse;
import com.gangwon.companion.domain.lodging.dto.LodgingSearchCriteria;
import com.gangwon.companion.domain.lodging.entity.Lodging;
import com.gangwon.companion.domain.lodging.entity.LodgingReview;
import com.gangwon.companion.domain.lodging.repository.LodgingRepository;
import com.gangwon.companion.domain.lodging.repository.LodgingReviewRepository;
import com.gangwon.companion.domain.lodging.repository.LodgingSpecifications;
import com.gangwon.companion.global.exception.BusinessException;
import com.gangwon.companion.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LodgingService {

    private final LodgingRepository lodgingRepository;
    private final LodgingReviewRepository lodgingReviewRepository;

    @Transactional(readOnly = true)
    public LodgingListResponse searchLodgings(LodgingSearchCriteria criteria) {
        Page<Lodging> result = lodgingRepository.findAll(
                LodgingSpecifications.from(criteria),
                criteria.pageable()
        );

        List<LodgingItemResponse> items = result.getContent().stream()
                .map(LodgingItemResponse::new)
                .toList();

        return new LodgingListResponse(result.getTotalElements(), items);
    }

    @Transactional(readOnly = true)
    public LodgingDetailResponse getLodgingDetail(Long lodgingId) {
        Lodging lodging = findLodgingWithPhotos(lodgingId);
        List<LodgingReview> reviews = lodgingReviewRepository.findByLodgingId(lodgingId);

        return new LodgingDetailResponse(lodging, reviews);
    }

    private Lodging findLodgingWithPhotos(Long lodgingId) {
        return lodgingRepository.findByIdWithPhotos(lodgingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }
}
