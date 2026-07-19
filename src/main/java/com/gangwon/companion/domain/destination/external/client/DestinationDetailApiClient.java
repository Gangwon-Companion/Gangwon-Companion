package com.gangwon.companion.domain.destination.external.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gangwon.companion.domain.destination.entity.SourceType;
import com.gangwon.companion.domain.destination.external.dto.detailApi.DetailApiResponse;
import com.gangwon.companion.domain.destination.external.dto.detailApi.DetailCommonItem;
import com.gangwon.companion.domain.destination.external.dto.detailApi.DetailImageItem;
import com.gangwon.companion.domain.destination.external.dto.detailApi.DetailIntroItem;
import com.gangwon.companion.domain.destination.external.dto.detailApi.DetailPetTourItem;
import com.gangwon.companion.domain.destination.external.dto.detailApi.DetailWithTourItem;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class DestinationDetailApiClient {

    private final RestClient tourApiRestClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${tour-api.service-key}")
    private String serviceKey;

    @Value("${tour-api.mobile-os:ETC}")
    private String mobileOs;

    @Value("${tour-api.mobile-app:GangwonCompanion}")
    private String mobileApp;

    public DetailApiResponse<DetailCommonItem> fetchDetailCommon(SourceType sourceType, Long contentId) {
        validateServiceKey();

        String responseBody = tourApiRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(servicePath(sourceType) + "/detailCommon2")
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("MobileOS", mobileOs)
                        .queryParam("MobileApp", mobileApp)
                        .queryParam("_type", "json")
                        .queryParam("contentId", contentId)
                        .build())
                .retrieve()
                .body(String.class);

        return readResponse(responseBody, new TypeReference<>() {
        });
    }

    public DetailApiResponse<DetailIntroItem> fetchDetailIntro(
            SourceType sourceType,
            Long contentId,
            Integer contentTypeId
    ) {
        validateServiceKey();

        String responseBody = tourApiRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(servicePath(sourceType) + "/detailIntro2")
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("MobileOS", mobileOs)
                        .queryParam("MobileApp", mobileApp)
                        .queryParam("_type", "json")
                        .queryParam("contentId", contentId)
                        .queryParam("contentTypeId", contentTypeId)
                        .build())
                .retrieve()
                .body(String.class);

        return readResponse(responseBody, new TypeReference<>() {
        });
    }

    public DetailApiResponse<DetailImageItem> fetchDetailImages(SourceType sourceType, Long contentId) {
        validateServiceKey();

        String responseBody = tourApiRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(servicePath(sourceType) + "/detailImage2")
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("MobileOS", mobileOs)
                        .queryParam("MobileApp", mobileApp)
                        .queryParam("_type", "json")
                        .queryParam("contentId", contentId)
                        .build())
                .retrieve()
                .body(String.class);

        return readResponse(responseBody, new TypeReference<>() {
        });
    }

    public DetailApiResponse<DetailPetTourItem> fetchDetailPetTour(Long contentId) {
        validateServiceKey();

        String responseBody = tourApiRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(servicePath(SourceType.PET) + "/detailPetTour2")
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("MobileOS", mobileOs)
                        .queryParam("MobileApp", mobileApp)
                        .queryParam("_type", "json")
                        .queryParam("contentId", contentId)
                        .build())
                .retrieve()
                .body(String.class);

        return readResponse(responseBody, new TypeReference<>() {
        });
    }

    public DetailApiResponse<DetailWithTourItem> fetchDetailWithTour(Long contentId) {
        validateServiceKey();

        String responseBody = tourApiRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(servicePath(SourceType.ACCESSIBILITY) + "/detailWithTour2")
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("MobileOS", mobileOs)
                        .queryParam("MobileApp", mobileApp)
                        .queryParam("_type", "json")
                        .queryParam("contentId", contentId)
                        .build())
                .retrieve()
                .body(String.class);

        return readResponse(responseBody, new TypeReference<>() {
        });
    }

    private <T> DetailApiResponse<T> readResponse(
            String responseBody,
            TypeReference<DetailApiResponse<T>> typeReference
    ) {
        try {
            return objectMapper.readValue(normalizeEmptyItems(responseBody), typeReference);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse detail API response.", e);
        }
    }

    private String normalizeEmptyItems(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "{\"response\":{\"body\":{\"items\":{\"item\":[]},\"totalCount\":0}}}";
        }

        return responseBody
                .replaceAll("\"items\"\\s*:\\s*\"\"", "\"items\":{\"item\":[]}")
                .replaceAll("\"item\"\\s*:\\s*\"\"", "\"item\":[]");
    }

    private void validateServiceKey() {
        if (serviceKey == null || serviceKey.isBlank()) {
            throw new IllegalStateException("TOUR_API_SERVICE_KEY is required.");
        }
    }

    private String servicePath(SourceType sourceType) {
        return switch (sourceType) {
            case KOREAN -> "/B551011/KorService2";
            case PET -> "/B551011/KorPetTourService2";
            case ACCESSIBILITY -> "/B551011/KorWithService2";
        };
    }
}
