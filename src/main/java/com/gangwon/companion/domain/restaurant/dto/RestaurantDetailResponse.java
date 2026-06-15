package com.gangwon.companion.domain.restaurant.dto;

import com.gangwon.companion.domain.restaurant.entity.Restaurant;
import com.gangwon.companion.domain.restaurant.entity.RestaurantReview;
import com.gangwon.companion.global.web.ReviewResponse;
import lombok.Getter;

import java.util.List;

@Getter
public class RestaurantDetailResponse {

    private final Long restaurantId;
    private final String name;
    private final String menuType;
    private final String region;
    private final Double rating;
    private final String address;
    private final List<String> photos;
    private final List<ReviewResponse> reviews;

    public RestaurantDetailResponse(Restaurant restaurant, List<RestaurantReview> reviews) {
        this.restaurantId = restaurant.getId();
        this.name = restaurant.getName();
        this.menuType = restaurant.getMenuType();
        this.region = restaurant.getRegion();
        this.rating = restaurant.getRating();
        this.address = restaurant.getAddress();
        this.photos = restaurant.getPhotos().stream()
                .map(p -> p.getUrl())
                .toList();
        this.reviews = reviews.stream()
                .map(review -> new ReviewResponse(
                        review.getId(),
                        review.getNickname(),
                        review.getContent(),
                        review.getRating(),
                        review.getCreatedAt()
                ))
                .toList();
    }
}
