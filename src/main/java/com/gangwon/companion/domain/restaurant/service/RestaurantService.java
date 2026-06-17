package com.gangwon.companion.domain.restaurant.service;

import com.gangwon.companion.domain.restaurant.dto.RestaurantDetailResponse;
import com.gangwon.companion.domain.restaurant.dto.RestaurantItemResponse;
import com.gangwon.companion.domain.restaurant.dto.RestaurantListResponse;
import com.gangwon.companion.domain.restaurant.dto.RestaurantSearchCriteria;
import com.gangwon.companion.domain.restaurant.entity.Restaurant;
import com.gangwon.companion.domain.restaurant.entity.RestaurantReview;
import com.gangwon.companion.domain.restaurant.repository.RestaurantRepository;
import com.gangwon.companion.domain.restaurant.repository.RestaurantReviewRepository;
import com.gangwon.companion.domain.restaurant.repository.RestaurantSpecifications;
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

    private Restaurant findRestaurantWithPhotos(Long restaurantId) {
        return restaurantRepository.findByIdWithPhotos(restaurantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }
}
