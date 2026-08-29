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
    private final Double rating;
    private final Long reviewCount;
    private final String roomCount;
    private final String roomType;
    private final String checkInTime;
    private final String checkOutTime;
    private final String parking;
    private final String subFacility;
    private final String infoCenter;
    private final List<String> photos;
    private final List<LodgingReviewResponse> reviews;
    private final LocationResponse location;

    public LodgingDetailResponse(Lodging lodging, List<LodgingReview> reviews) {
        this.lodgingId = lodging.getId();
        this.name = lodging.getName();
        this.description = lodging.getDescription();
        this.region = lodging.getRegion();
        this.rating = lodging.getRating();
        this.reviewCount = lodging.getReviewCount();
        this.roomCount = lodging.getRoomCount();
        this.roomType = lodging.getRoomType();
        this.checkInTime = lodging.getCheckInTime();
        this.checkOutTime = lodging.getCheckOutTime();
        this.parking = lodging.getParking();
        this.subFacility = lodging.getSubFacility();
        this.infoCenter = lodging.getInfoCenter();
        List<String> detailPhotos = lodging.getPhotos().stream()
                .map(p -> p.getOriginImgUrl() == null ? p.getUrl() : p.getOriginImgUrl())
                .filter(url -> url != null && !url.isBlank())
                .toList();
        this.photos = detailPhotos.isEmpty() && lodging.getThumbnailUrl() != null && !lodging.getThumbnailUrl().isBlank()
                ? List.of(lodging.getThumbnailUrl())
                : detailPhotos;
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
