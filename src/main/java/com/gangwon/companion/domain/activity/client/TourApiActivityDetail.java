package com.gangwon.companion.domain.activity.client;

public record TourApiActivityDetail(
        String homepageUrl,
        String overview,
        String telephone,
        String operatingHours,
        String restDate,
        String usageFee,
        String parkingInfo,
        String reservationUrl
) {
}
