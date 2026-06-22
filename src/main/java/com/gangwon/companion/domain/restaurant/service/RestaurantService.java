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
    public RestaurantDetailResponse getRestaurantDetail(Long restaurantId) {
        Restaurant restaurant = findRestaurantWithPhotos(restaurantId);
        List<RestaurantReview> reviews = restaurantReviewRepository.findByRestaurantId(restaurantId);

        return new RestaurantDetailResponse(restaurant, reviews);
    }

    @Transactional
    public RestaurantReviewResponse createReview(Long restaurantId, String username, RestaurantReviewRequest request) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        User user = findUserByUsername(username);

        RestaurantReview review = RestaurantReview.builder()
                .restaurant(restaurant)
                .user(user)
                .content(request.content())
                .rating(request.rating())
                .build();

        RestaurantReview saved = restaurantReviewRepository.save(review);
        return toReviewResponse(saved);
    }

    @Transactional
    public RestaurantReviewResponse updateReview(Long restaurantId, Long reviewId, String username, RestaurantReviewRequest request) {
        RestaurantReview review = findReviewByIdAndRestaurantId(reviewId, restaurantId);
        checkOwnership(review.getUser().getUsername(), username);

        review.update(request.content(), request.rating());
        return toReviewResponse(review);
    }

    @Transactional
    public void deleteReview(Long restaurantId, Long reviewId, String username) {
        RestaurantReview review = findReviewByIdAndRestaurantId(reviewId, restaurantId);
        checkOwnership(review.getUser().getUsername(), username);

        restaurantReviewRepository.delete(review);
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

    private RestaurantReviewResponse toReviewResponse(RestaurantReview review) {
        return new RestaurantReviewResponse(
                review.getId(),
                review.getUser().getNickname(),
                review.getContent(),
                review.getRating(),
                review.getCreatedAt()
        );
    }
}
