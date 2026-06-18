package com.gangwon.companion.domain.lodging.service;

import com.gangwon.companion.domain.lodging.dto.LodgingDetailResponse;
import com.gangwon.companion.domain.lodging.dto.LodgingItemResponse;
import com.gangwon.companion.domain.lodging.dto.LodgingListResponse;
import com.gangwon.companion.domain.lodging.dto.LodgingReviewRequest;
import com.gangwon.companion.domain.lodging.dto.LodgingReviewResponse;
import com.gangwon.companion.domain.lodging.dto.LodgingSearchCriteria;
import com.gangwon.companion.domain.lodging.entity.Lodging;
import com.gangwon.companion.domain.lodging.entity.LodgingReview;
import com.gangwon.companion.domain.lodging.repository.LodgingRepository;
import com.gangwon.companion.domain.lodging.repository.LodgingReviewRepository;
import com.gangwon.companion.domain.lodging.repository.LodgingSpecifications;
import com.gangwon.companion.domain.user.entity.User;
import com.gangwon.companion.domain.user.repository.UserRepository;
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
    private final UserRepository userRepository;

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

    @Transactional
    public LodgingReviewResponse createReview(Long lodgingId, String username, LodgingReviewRequest request) {
        Lodging lodging = lodgingRepository.findById(lodgingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        User user = findUserByUsername(username);

        LodgingReview review = LodgingReview.builder()
                .lodging(lodging)
                .user(user)
                .content(request.content())
                .rating(request.rating())
                .build();

        LodgingReview saved = lodgingReviewRepository.save(review);
        return toReviewResponse(saved);
    }

    @Transactional
    public LodgingReviewResponse updateReview(Long lodgingId, Long reviewId, String username, LodgingReviewRequest request) {
        LodgingReview review = findReviewByIdAndLodgingId(reviewId, lodgingId);
        checkOwnership(review.getUser().getUsername(), username);

        review.update(request.content(), request.rating());
        return toReviewResponse(review);
    }

    @Transactional
    public void deleteReview(Long lodgingId, Long reviewId, String username) {
        LodgingReview review = findReviewByIdAndLodgingId(reviewId, lodgingId);
        checkOwnership(review.getUser().getUsername(), username);

        lodgingReviewRepository.delete(review);
    }

    private Lodging findLodgingWithPhotos(Long lodgingId) {
        return lodgingRepository.findByIdWithPhotos(lodgingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private LodgingReview findReviewByIdAndLodgingId(Long reviewId, Long lodgingId) {
        return lodgingReviewRepository.findByIdAndLodgingId(reviewId, lodgingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));
    }

    private void checkOwnership(String ownerUsername, String requestUsername) {
        if (!ownerUsername.equals(requestUsername)) {
            throw new BusinessException(ErrorCode.REVIEW_FORBIDDEN);
        }
    }

    private LodgingReviewResponse toReviewResponse(LodgingReview review) {
        return new LodgingReviewResponse(
                review.getId(),
                review.getUser().getNickname(),
                review.getContent(),
                review.getRating(),
                review.getCreatedAt()
        );
    }
}
