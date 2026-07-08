package com.gangwon.companion.domain.activity.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class TourApiActivityClient {

    private static final DateTimeFormatter TOUR_API_DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Pattern HREF_PATTERN = Pattern.compile("href=[\\\"']([^\\\"']+)[\\\"']", Pattern.CASE_INSENSITIVE);

    private final RestClient restClient;

    @Value("${tour-api.service-key}")
    private String serviceKey;

    @Value("${tour-api.base-url:https://apis.data.go.kr}")
    private String baseUrl;

    public List<TourApiActivityItem> getGangwonActivities(int contentTypeId, int rows, int maxPages) {
        List<TourApiActivityItem> result = new ArrayList<>();

        for (int page = 1; page <= maxPages; page++) {
            JsonNode response = requestAreaBasedList(contentTypeId, rows, page);
            JsonNode body = response.path("response").path("body");
            JsonNode items = body.path("items").path("item");
            result.addAll(toActivityItems(items, contentTypeId));

            int totalCount = body.path("totalCount").asInt(0);
            if (result.size() >= totalCount || items.isMissingNode() || items.isNull()) {
                break;
            }
        }

        return result;
    }

    public TourApiActivityDetail getActivityDetail(Long contentId, int contentTypeId) {
        JsonNode commonItem = extractFirstItem(requestDetailCommon(contentId));
        JsonNode introItem = extractFirstItem(requestDetailIntro(contentId, contentTypeId));

        return new TourApiActivityDetail(
                extractUrl(text(commonItem, "homepage")),
                cleanHtml(text(commonItem, "overview")),
                firstNonBlank(text(commonItem, "tel"), infoCenter(introItem, contentTypeId)),
                operatingHours(introItem, contentTypeId),
                restDate(introItem, contentTypeId),
                usageFee(introItem, contentTypeId),
                parkingInfo(introItem, contentTypeId),
                extractUrl(firstNonBlank(text(introItem, "reservation"), text(introItem, "reservationurl")))
        );
    }

    private JsonNode requestAreaBasedList(int contentTypeId, int rows, int page) {
        return restClient.get()
                .uri(uriBuilder -> commonUri(uriBuilder.path("/B551011/KorService2/areaBasedList2"))
                        .queryParam("areaCode", "32")
                        .queryParam("contentTypeId", contentTypeId)
                        .queryParam("arrange", "O")
                        .queryParam("numOfRows", rows)
                        .queryParam("pageNo", page)
                        .build())
                .retrieve()
                .body(JsonNode.class);
    }

    private JsonNode requestDetailCommon(Long contentId) {
        return restClient.get()
                .uri(uriBuilder -> commonUri(uriBuilder.path("/B551011/KorService2/detailCommon2"))
                        .queryParam("contentId", contentId)
                        .queryParam("numOfRows", 10)
                        .queryParam("pageNo", 1)
                        .build())
                .retrieve()
                .body(JsonNode.class);
    }

    private JsonNode requestDetailIntro(Long contentId, int contentTypeId) {
        return restClient.get()
                .uri(uriBuilder -> commonUri(uriBuilder.path("/B551011/KorService2/detailIntro2"))
                        .queryParam("contentId", contentId)
                        .queryParam("contentTypeId", contentTypeId)
                        .queryParam("numOfRows", 10)
                        .queryParam("pageNo", 1)
                        .build())
                .retrieve()
                .body(JsonNode.class);
    }

    private org.springframework.web.util.UriBuilder commonUri(org.springframework.web.util.UriBuilder uriBuilder) {
        return uriBuilder
                .scheme("https")
                .host(baseUrl.replace("https://", "").replace("http://", ""))
                .queryParam("serviceKey", serviceKey)
                .queryParam("MobileOS", "ETC")
                .queryParam("MobileApp", "GangwonCompanion")
                .queryParam("_type", "json");
    }

    private List<TourApiActivityItem> toActivityItems(JsonNode items, int contentTypeId) {
        if (items.isMissingNode() || items.isNull()) {
            return List.of();
        }

        List<TourApiActivityItem> result = new ArrayList<>();
        if (items.isArray()) {
            for (JsonNode item : items) {
                result.add(toActivityItem(item, contentTypeId));
            }
        } else {
            result.add(toActivityItem(items, contentTypeId));
        }
        return result;
    }

    private TourApiActivityItem toActivityItem(JsonNode item, int contentTypeId) {
        return new TourApiActivityItem(
                longValue(item, "contentid"),
                contentTypeId,
                text(item, "cat2"),
                text(item, "title"),
                text(item, "addr1"),
                text(item, "firstimage"),
                text(item, "firstimage2"),
                text(item, "tel"),
                doubleValue(item, "mapy"),
                doubleValue(item, "mapx"),
                dateTime(item, "modifiedtime")
        );
    }

    private JsonNode extractFirstItem(JsonNode response) {
        if (response == null) {
            return null;
        }
        JsonNode item = response.path("response").path("body").path("items").path("item");
        if (item.isArray()) {
            return item.isEmpty() ? null : item.get(0);
        }
        return item.isMissingNode() || item.isNull() ? null : item;
    }

    private String operatingHours(JsonNode item, int contentTypeId) {
        return switch (contentTypeId) {
            case 14 -> cleanHtml(text(item, "usetimeculture"));
            case 28 -> cleanHtml(text(item, "usetimeleports"));
            default -> cleanHtml(text(item, "usetime"));
        };
    }

    private String restDate(JsonNode item, int contentTypeId) {
        return switch (contentTypeId) {
            case 14 -> cleanHtml(text(item, "restdateculture"));
            case 28 -> cleanHtml(text(item, "restdateleports"));
            default -> cleanHtml(text(item, "restdate"));
        };
    }

    private String usageFee(JsonNode item, int contentTypeId) {
        return switch (contentTypeId) {
            case 14 -> cleanHtml(text(item, "usefee"));
            case 28 -> cleanHtml(text(item, "usefeeleports"));
            default -> cleanHtml(text(item, "useseason"));
        };
    }

    private String parkingInfo(JsonNode item, int contentTypeId) {
        return switch (contentTypeId) {
            case 14 -> cleanHtml(text(item, "parkingculture"));
            case 28 -> cleanHtml(text(item, "parkingleports"));
            default -> cleanHtml(text(item, "parking"));
        };
    }

    private String infoCenter(JsonNode item, int contentTypeId) {
        return switch (contentTypeId) {
            case 14 -> text(item, "infocenterculture");
            case 28 -> text(item, "infocenterleports");
            default -> text(item, "infocenter");
        };
    }

    private String extractUrl(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        Matcher matcher = HREF_PATTERN.matcher(value);
        return HtmlUtils.htmlUnescape(matcher.find() ? matcher.group(1) : cleanHtml(value));
    }

    private String cleanHtml(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return HtmlUtils.htmlUnescape(value.replaceAll("<[^>]+>", " "))
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String text(JsonNode item, String field) {
        if (item == null) {
            return null;
        }
        String value = item.path(field).asText("").trim();
        return value.isBlank() ? null : value;
    }

    private Long longValue(JsonNode item, String field) {
        String value = text(item, field);
        return value == null ? null : Long.valueOf(value);
    }

    private Double doubleValue(JsonNode item, String field) {
        String value = text(item, field);
        return value == null ? null : Double.valueOf(value);
    }

    private LocalDateTime dateTime(JsonNode item, String field) {
        String value = text(item, field);
        if (value == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, TOUR_API_DATE_TIME);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }
}
