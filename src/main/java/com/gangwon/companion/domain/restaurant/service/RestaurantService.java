package com.gangwon.companion.domain.restaurant.service;

import com.gangwon.companion.domain.restaurant.dto.RestaurantDetailResponse;
import com.gangwon.companion.domain.restaurant.dto.RestaurantItemResponse;
import com.gangwon.companion.domain.restaurant.dto.RestaurantListResponse;
import com.gangwon.companion.domain.restaurant.entity.Restaurant;
import com.gangwon.companion.domain.restaurant.entity.RestaurantReview;
import com.gangwon.companion.domain.restaurant.query.RestaurantSearchCriteria;
import com.gangwon.companion.domain.restaurant.query.RestaurantSpecifications;
import com.gangwon.companion.domain.restaurant.repository.RestaurantRepository;
import com.gangwon.companion.domain.restaurant.repository.RestaurantReviewRepository;
import com.gangwon.companion.global.exception.ResourceNotFoundException;
import com.gangwon.companion.global.route.RouteResponse;
import com.gangwon.companion.global.route.RouteResponseFactory;
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
    private final RouteResponseFactory routeResponseFactory;

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

    @Transactional(readOnly = true)
    public RouteResponse getRoute(Long restaurantId, Double userLat, Double userLng) {
        Restaurant restaurant = findRestaurant(restaurantId);

        return routeResponseFactory.create(
                userLat,
                userLng,
                restaurant.getLatitude(),
                restaurant.getLongitude(),
                restaurant.getName()
        );
    }

    private Restaurant findRestaurant(Long restaurantId) {
        return restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 식당을 찾을 수 없습니다."));
    }

    private Restaurant findRestaurantWithPhotos(Long restaurantId) {
        return restaurantRepository.findByIdWithPhotos(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 식당을 찾을 수 없습니다."));
    }
}
