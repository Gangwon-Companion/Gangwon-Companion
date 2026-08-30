package com.gangwon.companion.domain.restaurant.service;

import com.gangwon.companion.domain.restaurant.dto.request.RestaurantReviewRequest;
import com.gangwon.companion.domain.restaurant.dto.request.RestaurantSearchCriteria;
import com.gangwon.companion.domain.restaurant.dto.response.RestaurantDetailResponse;
import com.gangwon.companion.domain.restaurant.dto.response.RestaurantItemResponse;
import com.gangwon.companion.domain.restaurant.dto.response.RestaurantListResponse;
import com.gangwon.companion.domain.restaurant.dto.response.RestaurantReviewResponse;
import com.gangwon.companion.domain.restaurant.entity.Restaurant;
import com.gangwon.companion.domain.restaurant.entity.RestaurantReview;
import com.gangwon.companion.domain.restaurant.repository.RestaurantRepository;
import com.gangwon.companion.domain.restaurant.repository.RestaurantReviewRepository;
import com.gangwon.companion.domain.restaurant.repository.RestaurantSpecifications;
import com.gangwon.companion.domain.user.entity.User;
import com.gangwon.companion.domain.user.repository.UserRepository;
import com.gangwon.companion.global.exception.BusinessException;
import com.gangwon.companion.global.exception.ErrorCode;
import com.gangwon.companion.global.storage.S3FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantReviewRepository restaurantReviewRepository;
    private final UserRepository userRepository;
    private final S3FileService s3FileService;

    @Transactional(readOnly = true)
    public RestaurantListResponse searchRestaurants(RestaurantSearchCriteria criteria) {
        Page<Restaurant> result = restaurantRepository.findAll(
                RestaurantSpecifications.from(criteria),
                criteria.pageable()
        );

        List<RestaurantItemResponse> items = result.getContent().stream()
                .map(RestaurantItemResponse::new)
                .toList();

        return new RestaurantListResponse(result.getTotalElements(), items);
    }

    @Transactional(readOnly = true)
    public RestaurantDetailResponse getRestaurantDetail(Long restaurantId, String username) {
        Restaurant restaurant = findRestaurantWithPhotos(restaurantId);
        List<RestaurantReview> reviews = restaurantReviewRepository.findByRestaurantId(restaurantId);

        return new RestaurantDetailResponse(restaurant, reviews, username, this::profileImageUrl);
    }

    @Transactional
    public RestaurantReviewResponse createReview(Long restaurantId, String username, RestaurantReviewRequest request) {
        Restaurant restaurant = restaurantRepository.findByIdForUpdate(restaurantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        User user = findUserByUsername(username);

        RestaurantReview review = RestaurantReview.builder()
                .restaurant(restaurant)
                .user(user)
                .content(request.content())
                .rating(request.rating())
                .build();

        RestaurantReview saved = restaurantReviewRepository.save(review);
        updateRestaurantReviewStats(restaurant);
        return toReviewResponse(saved, username);
    }

    @Transactional
    public RestaurantReviewResponse updateReview(Long restaurantId, Long reviewId, String username, RestaurantReviewRequest request) {
        RestaurantReview review = findReviewByIdAndRestaurantId(reviewId, restaurantId);
        checkOwnership(review.getUser().getUsername(), username);
        Restaurant restaurant = restaurantRepository.findByIdForUpdate(restaurantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        review.update(request.content(), request.rating());
        updateRestaurantReviewStats(restaurant);
        return toReviewResponse(review, username);
    }

    @Transactional
    public void deleteReview(Long restaurantId, Long reviewId, String username) {
        RestaurantReview review = findReviewByIdAndRestaurantId(reviewId, restaurantId);
        checkOwnership(review.getUser().getUsername(), username);
        Restaurant restaurant = restaurantRepository.findByIdForUpdate(restaurantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        restaurantReviewRepository.delete(review);
        updateRestaurantReviewStats(restaurant);
    }

    private Restaurant findRestaurantWithPhotos(Long restaurantId) {
        return restaurantRepository.findByIdWithPhotos(restaurantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private RestaurantReview findReviewByIdAndRestaurantId(Long reviewId, Long restaurantId) {
        return restaurantReviewRepository.findByIdAndRestaurantId(reviewId, restaurantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));
    }

    private void checkOwnership(String ownerUsername, String requestUsername) {
        if (!ownerUsername.equals(requestUsername)) {
            throw new BusinessException(ErrorCode.REVIEW_FORBIDDEN);
        }
    }

    private void updateRestaurantReviewStats(Restaurant restaurant) {
        Long restaurantId = restaurant.getId();
        Double averageRating = restaurantReviewRepository.calculateAverageRatingByRestaurantId(restaurantId);
        long reviewCount = restaurantReviewRepository.countByRestaurantId(restaurantId);
        restaurant.updateReviewStats(roundToOneDecimal(averageRating), reviewCount);
    }

    private double roundToOneDecimal(Double value) {
        if (value == null) {
            return 0.0;
        }
        return Math.round(value * 10.0) / 10.0;
    }

    private RestaurantReviewResponse toReviewResponse(RestaurantReview review, String username) {
        return new RestaurantReviewResponse(
                review.getId(),
                review.getUser().getNickname(),
                profileImageUrl(review.getUser()),
                review.getContent(),
                review.getRating(),
                review.getCreatedAt(),
                username != null && review.getUser().getUsername().equals(username)
        );
    }

    private String profileImageUrl(User user) {
        String key = user.getProfileImageS3Key();
        return key == null || key.isBlank() ? null : s3FileService.createDownloadUrl(key);
    }
}
