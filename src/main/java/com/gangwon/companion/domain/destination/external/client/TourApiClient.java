package com.gangwon.companion.domain.destination.external.client;

import com.gangwon.companion.domain.destination.external.dto.destinationApi.TourApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class TourApiClient {

    private final RestClient tourApiRestClient;

    @Value("${tour-api.service-key}")
    private String serviceKey;

    @Value("${tour-api.mobile-os:ETC}")
    private String mobileOs;

    @Value("${tour-api.mobile-app:GangwonCompanion}")
    private String mobileApp;

    @Value("${tour-api.area-code:32}")
    private String areaCode;

    public TourApiResponse fetchDestinations(int pageNo, int numOfRows) {
        if (serviceKey == null || serviceKey.isBlank()) {
            throw new IllegalStateException("TOUR_API_SERVICE_KEY is required.");
        }

        return tourApiRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/B551011/KorService2/areaBasedList2")
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("MobileOS", mobileOs)
                        .queryParam("MobileApp", mobileApp)
                        .queryParam("_type", "json")
                        .queryParam("areaCode", areaCode)
                        .queryParam("pageNo", pageNo)
                        .queryParam("numOfRows", numOfRows)
                        .build())
                .retrieve()
                .body(TourApiResponse.class);
    }
}
