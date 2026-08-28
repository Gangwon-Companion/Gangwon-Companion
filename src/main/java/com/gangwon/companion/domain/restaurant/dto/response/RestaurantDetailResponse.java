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
    private final String firstMenu;
    private final String treatMenu;
    private final String openTime;
    private final String restDate;
    private final String parking;
    private final String infoCenter;
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
        this.firstMenu = restaurant.getFirstMenu();
        this.treatMenu = restaurant.getTreatMenu();
        this.openTime = restaurant.getOpenTime();
        this.restDate = restaurant.getRestDate();
        this.parking = restaurant.getParking();
        this.infoCenter = restaurant.getInfoCenter();
        List<String> detailPhotos = restaurant.getPhotos().stream()
                .map(p -> p.getOriginImgUrl() == null ? p.getUrl() : p.getOriginImgUrl())
                .filter(url -> url != null && !url.isBlank())
                .toList();
        this.photos = detailPhotos.isEmpty() && restaurant.getThumbnailUrl() != null && !restaurant.getThumbnailUrl().isBlank()
                ? List.of(restaurant.getThumbnailUrl())
                : detailPhotos;
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
