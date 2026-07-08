package com.gangwon.companion.domain.home.client;

import com.gangwon.companion.domain.home.dto.PromotionBannerResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TourApiClient {

    private static final Logger log = LoggerFactory.getLogger(TourApiClient.class);
    private static final DateTimeFormatter TOUR_API_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final RestClient restClient;

    @Value("${tour-api.service-key}")
    private String serviceKey;

    @Value("${tour-api.base-url:https://apis.data.go.kr}")
    private String baseUrl;

    public List<PromotionBannerResponse> getGangwonFestivalBanners(int limit) {
        LocalDate currentYearStart = LocalDate.now().withDayOfYear(1);
        LocalDate today = LocalDate.now();
        List<PromotionBannerResponse> currentYearBanners = getGangwonFestivalBanners(limit, currentYearStart, today);
        if (!currentYearBanners.isEmpty()) {
            return currentYearBanners;
        }

        return getGangwonFestivalBanners(limit, currentYearStart.minusYears(1), today);
    }

    public List<PromotionBannerResponse> getGangwonSpotBanners(int limit) {
        JsonNode response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host(baseUrl.replace("https://", "").replace("http://", ""))
                        .path("/B551011/KorService2/areaBasedList2")
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("MobileOS", "ETC")
                        .queryParam("MobileApp", "GangwonCompanion")
                        .queryParam("_type", "json")
                        .queryParam("areaCode", "32")
                        .queryParam("contentTypeId", "12")
                        .queryParam("arrange", "Q")
                        .queryParam("numOfRows", limit)
                        .queryParam("pageNo", 1)
                        .build())
                .retrieve()
                .body(JsonNode.class);

        return toSpotBanners(response);
    }

    private List<PromotionBannerResponse> getGangwonFestivalBanners(
            int limit,
            LocalDate eventStartDate,
            LocalDate minimumEndDate
    ) {
        int requestRows = Math.max(limit * 5, 20);

        JsonNode response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host(baseUrl.replace("https://", "").replace("http://", ""))
                        .path("/B551011/KorService2/searchFestival2")
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("MobileOS", "ETC")
                        .queryParam("MobileApp", "GangwonCompanion")
                        .queryParam("_type", "json")
                        .queryParam("areaCode", "32")
                        .queryParam("eventStartDate", eventStartDate.format(TOUR_API_DATE))
                        .queryParam("arrange", "O")
                        .queryParam("numOfRows", requestRows)
                        .queryParam("pageNo", 1)
                        .build())
                .retrieve()
                .body(JsonNode.class);

        return toPromotionBanners(response, minimumEndDate).stream()
                .limit(limit)
                .toList();
    }

    private List<PromotionBannerResponse> toPromotionBanners(JsonNode response, LocalDate minimumEndDate) {
        JsonNode header = response.path("response").path("header");
        String resultCode = header.path("resultCode").asText("");
        String resultMessage = header.path("resultMsg").asText("");
        if (!resultCode.isBlank() && !"0000".equals(resultCode)) {
            log.warn("TourAPI searchFestival2 failed. resultCode={}, resultMessage={}", resultCode, resultMessage);
            return List.of();
        }

        JsonNode items = response.path("response").path("body").path("items").path("item");
        if (items.isMissingNode() || items.isNull()) {
            log.warn("TourAPI searchFestival2 returned no items. response={}", response);
            return List.of();
        }

        List<PromotionBannerResponse> banners = new ArrayList<>();
        if (items.isArray()) {
            for (JsonNode item : items) {
                PromotionBannerResponse banner = toPromotionBanner(item);
                if (isActiveOrUpcomingEvent(banner, minimumEndDate)) {
                    banners.add(banner);
                }
            }
            return banners;
        }

        PromotionBannerResponse banner = toPromotionBanner(items);
        if (isActiveOrUpcomingEvent(banner, minimumEndDate)) {
            banners.add(banner);
        }
        return banners;
    }

    private boolean isActiveOrUpcomingEvent(PromotionBannerResponse banner, LocalDate minimumEndDate) {
        return banner.endDate() != null && !banner.endDate().isBefore(minimumEndDate);
    }

    private List<PromotionBannerResponse> toSpotBanners(JsonNode response) {
        JsonNode header = response.path("response").path("header");
        String resultCode = header.path("resultCode").asText("");
        String resultMessage = header.path("resultMsg").asText("");
        if (!resultCode.isBlank() && !"0000".equals(resultCode)) {
            log.warn("TourAPI areaBasedList2 failed. resultCode={}, resultMessage={}", resultCode, resultMessage);
            return List.of();
        }

        JsonNode items = response.path("response").path("body").path("items").path("item");
        if (items.isMissingNode() || items.isNull()) {
            log.warn("TourAPI areaBasedList2 returned no items. response={}", response);
            return List.of();
        }

        List<PromotionBannerResponse> banners = new ArrayList<>();
        if (items.isArray()) {
            for (JsonNode item : items) {
                banners.add(toSpotBanner(item));
            }
            return banners;
        }

        banners.add(toSpotBanner(items));
        return banners;
    }

    private PromotionBannerResponse toPromotionBanner(JsonNode item) {
        Long contentId = asLong(item.path("contentid"));
        String title = asText(item.path("title"));
        String region = extractRegion(asText(item.path("addr1")));

        return new PromotionBannerResponse(
                contentId,
                "festival",
                title,
                asText(item.path("addr1")),
                region,
                firstNonBlank(asText(item.path("firstimage")), asText(item.path("firstimage2"))),
                parseDate(asText(item.path("eventstartdate"))),
                parseDate(asText(item.path("eventenddate"))),
                "/tour/festivals/" + contentId
        );
    }

    private PromotionBannerResponse toSpotBanner(JsonNode item) {
        Long contentId = asLong(item.path("contentid"));
        String title = asText(item.path("title"));
        String region = extractRegion(asText(item.path("addr1")));

        return new PromotionBannerResponse(
                contentId,
                "spot",
                title,
                asText(item.path("addr1")),
                region,
                firstNonBlank(asText(item.path("firstimage")), asText(item.path("firstimage2"))),
                null,
                null,
                "/tour/spots/" + contentId
        );
    }

    private String extractRegion(String address) {
        if (address == null || address.isBlank()) {
            return "gangwon";
        }
        String[] parts = address.split(" ");
        return parts.length > 1 ? parts[1] : parts[0];
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    private String asText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        return node.asText("");
    }

    private Long asLong(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull() || node.asText().isBlank()) {
            return null;
        }
        return node.asLong();
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value, TOUR_API_DATE);
    }
}
