package com.gangwon.companion.domain.destination.service;

import com.gangwon.companion.domain.destination.dto.DestinationDetailResponseDto;
import com.gangwon.companion.domain.destination.dto.DestinationImageResponseDto;
import com.gangwon.companion.domain.destination.dto.DestinationReviewRequest;
import com.gangwon.companion.domain.destination.dto.DestinationReviewResponse;
import com.gangwon.companion.domain.destination.dto.PetInfoResponseDto;
import com.gangwon.companion.domain.destination.dto.AccessibilityInfoResponseDto;
import com.gangwon.companion.domain.destination.entity.Destination;
import com.gangwon.companion.domain.destination.entity.DestinationDetail;
import com.gangwon.companion.domain.destination.entity.DestinationReview;
import com.gangwon.companion.domain.destination.entity.SourceType;
import com.gangwon.companion.domain.destination.repository.AccessibilityInfoRepository;
import com.gangwon.companion.domain.destination.repository.DestinationDetailRepository;
import com.gangwon.companion.domain.destination.repository.DestinationImageRepository;
import com.gangwon.companion.domain.destination.repository.DestinationRepository;
import com.gangwon.companion.domain.destination.repository.DestinationReviewRepository;
import com.gangwon.companion.domain.destination.repository.PetInfoRepository;
import com.gangwon.companion.domain.user.entity.User;
import com.gangwon.companion.domain.user.repository.UserRepository;
import com.gangwon.companion.global.exception.BusinessException;
import com.gangwon.companion.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DestinationDetailService {
    private final DestinationRepository destinationRepository;
    private final DestinationDetailRepository destinationDetailRepository;
    private final DestinationImageRepository destinationImageRepository;
    private final DestinationReviewRepository destinationReviewRepository;
    private final PetInfoRepository petInfoRepository;
    private final AccessibilityInfoRepository accessibilityInfoRepository;
    private final UserRepository userRepository;

    public DestinationDetailResponseDto getDestinationDetailByDestinationId(Long destinationId, boolean pet, boolean accessibility) {

        SourceType sourceType = resolveSourceType(pet, accessibility);
        DestinationDetail destinationDetail = destinationDetailRepository.findByDestinationIdAndSourceType(destinationId, sourceType)
                .orElseThrow(() -> new BusinessException(ErrorCode.DESTINATION_DETAIL_NOT_FOUND));

        List<DestinationImageResponseDto> destinationImageList = destinationImageRepository.findByDestinationIdAndSourceType(destinationId, sourceType).stream()
                .map(destinationImage -> DestinationImageResponseDto.from(destinationImage))
                .toList();

        PetInfoResponseDto petInfo = pet
                ? petInfoRepository.findByDestinationId(destinationId)
                .map(PetInfoResponseDto::from)
                .orElse(null)
                : null;

        AccessibilityInfoResponseDto accessibilityInfo = accessibility
                ? accessibilityInfoRepository.findByDestinationId(destinationId)
                .map(AccessibilityInfoResponseDto::from)
                .orElse(null)
                : null;

        List<DestinationReviewResponse> reviews = destinationReviewRepository.findByDestinationId(destinationId).stream()
                .map(this::toReviewResponse)
                .toList();

        return DestinationDetailResponseDto.from(destinationDetail, destinationImageList, petInfo, accessibilityInfo, reviews);
    }

    @Transactional
    public DestinationReviewResponse createReview(Long destinationId, String username, DestinationReviewRequest request) {
        Destination destination = destinationRepository.findByIdForUpdate(destinationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        DestinationReview review = DestinationReview.builder()
                .destination(destination)
                .user(user)
                .content(request.content())
                .rating(request.rating())
                .build();

        DestinationReview saved = destinationReviewRepository.save(review);
        updateDestinationReviewStats(destination);
        return toReviewResponse(saved);
    }

    @Transactional
    public DestinationReviewResponse updateReview(Long destinationId, Long reviewId, String username, DestinationReviewRequest request) {
        DestinationReview review = destinationReviewRepository.findByIdAndDestinationId(reviewId, destinationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));
        checkOwnership(review.getUser().getUsername(), username);
        Destination destination = destinationRepository.findByIdForUpdate(destinationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        review.update(request.content(), request.rating());
        updateDestinationReviewStats(destination);
        return toReviewResponse(review);
    }

    @Transactional
    public void deleteReview(Long destinationId, Long reviewId, String username) {
        DestinationReview review = destinationReviewRepository.findByIdAndDestinationId(reviewId, destinationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));
        checkOwnership(review.getUser().getUsername(), username);
        Destination destination = destinationRepository.findByIdForUpdate(destinationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        destinationReviewRepository.delete(review);
        updateDestinationReviewStats(destination);
    }

    private void updateDestinationReviewStats(Destination destination) {
        Long destinationId = destination.getId();
        Double averageRating = destinationReviewRepository.calculateAverageRatingByDestinationId(destinationId);
        long reviewCount = destinationReviewRepository.countByDestinationId(destinationId);
        destination.updateReviewStats(roundToOneDecimal(averageRating), reviewCount);
    }

    private void checkOwnership(String ownerUsername, String requestUsername) {
        if (!ownerUsername.equals(requestUsername)) {
            throw new BusinessException(ErrorCode.REVIEW_FORBIDDEN);
        }
    }

    private double roundToOneDecimal(Double value) {
        if (value == null) {
            return 0.0;
        }
        return Math.round(value * 10.0) / 10.0;
    }

    private DestinationReviewResponse toReviewResponse(DestinationReview review) {
        return new DestinationReviewResponse(
                review.getId(),
                review.getUser().getNickname(),
                review.getContent(),
                review.getRating(),
                review.getCreatedAt()
        );
    }

    private SourceType resolveSourceType(boolean pet, boolean accessibility) {
        if (pet && accessibility) {
            return SourceType.PET;
        }

        if (pet) {
            return SourceType.PET;
        }

        if (accessibility) {
            return SourceType.ACCESSIBILITY;
        }

        return SourceType.KOREAN;
    }
}
