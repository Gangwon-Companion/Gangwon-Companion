package com.gangwon.companion.domain.restaurant.dto.response;

import com.gangwon.companion.domain.restaurant.entity.Restaurant;
import com.gangwon.companion.domain.restaurant.entity.RestaurantReview;
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
    private final Double latitude;
    private final Double longitude;
    private final List<String> photos;
    private final List<RestaurantReviewResponse> reviews;

    public RestaurantDetailResponse(Restaurant restaurant, List<RestaurantReview> reviews) {
        this.restaurantId = restaurant.getId();
        this.name = restaurant.getName();
        this.menuType = restaurant.getMenuType();
        this.region = restaurant.getRegion();
        this.rating = restaurant.getRating();
        this.address = restaurant.getAddress();
        this.latitude = restaurant.getLatitude();
        this.longitude = restaurant.getLongitude();
        this.photos = restaurant.getPhotos().stream()
                .map(p -> p.getUrl())
                .toList();
        this.reviews = reviews.stream()
                .map(review -> new RestaurantReviewResponse(
                        review.getId(),
                        review.getUser().getNickname(),
                        review.getContent(),
                        review.getRating(),
                        review.getCreatedAt()
                ))
                .toList();
    }
}
