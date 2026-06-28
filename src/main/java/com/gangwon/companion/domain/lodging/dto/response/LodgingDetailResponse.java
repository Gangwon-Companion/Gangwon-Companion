package com.gangwon.companion.domain.lodging.dto.response;

import com.gangwon.companion.domain.lodging.entity.Lodging;
import com.gangwon.companion.domain.lodging.entity.LodgingReview;
import com.gangwon.companion.global.web.LocationResponse;
import lombok.Getter;

import java.util.List;

@Getter
public class LodgingDetailResponse {

    private final Long lodgingId;
    private final String name;
    private final String description;
    private final String region;
    private final Long price;
    private final Double rating;
    private final List<String> photos;
    private final List<LodgingReviewResponse> reviews;
    private final LocationResponse location;

    public LodgingDetailResponse(Lodging lodging, List<LodgingReview> reviews) {
        this.lodgingId = lodging.getId();
        this.name = lodging.getName();
        this.description = lodging.getDescription();
        this.region = lodging.getRegion();
        this.price = lodging.getPrice();
        this.rating = lodging.getRating();
        this.photos = lodging.getPhotos().stream()
                .map(p -> p.getUrl())
                .toList();
        this.reviews = reviews.stream()
                .map(review -> new LodgingReviewResponse(
                        review.getId(),
                        review.getUser().getNickname(),
                        review.getContent(),
                        review.getRating(),
                        review.getCreatedAt()
                ))
                .toList();
        this.location = new LocationResponse(lodging.getLatitude(), lodging.getLongitude(), lodging.getAddress());
    }
}
