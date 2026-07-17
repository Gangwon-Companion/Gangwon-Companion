package com.gangwon.companion.global.external.tourapi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gangwon.companion.global.config.TourApiProperties;
import com.gangwon.companion.global.external.tourapi.dto.TourApiItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class TourApiClient {

    private static final String GANGWON_REGION_CODE = "51";
    private static final String RESTAURANT_TYPE_ID = "39";

    private final TourApiProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public TourApiClient(TourApiProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.create();
        this.objectMapper = new ObjectMapper();
    }

    public List<TourApiItem> fetchRestaurants(int pageNo, int numOfRows) {
        URI uri = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .path("/areaBasedList2")
                .queryParam("serviceKey", properties.getServiceKey())
                .queryParam("numOfRows", numOfRows)
                .queryParam("pageNo", pageNo)
                .queryParam("MobileOS", "ETC")
                .queryParam("MobileApp", "GangwonCompanion")
                .queryParam("_type", "json")
                .queryParam("arrange", "C")
                .queryParam("contentTypeId", RESTAURANT_TYPE_ID)
                .queryParam("lDongRegnCd", GANGWON_REGION_CODE)
                .build(true)
                .toUri();

        return fetchItems(uri);
    }

    public List<TourApiItem> fetchLodgings(int pageNo, int numOfRows) {
        URI uri = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .path("/searchStay2")
                .queryParam("serviceKey", properties.getServiceKey())
                .queryParam("numOfRows", numOfRows)
                .queryParam("pageNo", pageNo)
                .queryParam("MobileOS", "ETC")
                .queryParam("MobileApp", "GangwonCompanion")
                .queryParam("_type", "json")
                .queryParam("arrange", "C")
                .queryParam("lDongRegnCd", GANGWON_REGION_CODE)
                .build(true)
                .toUri();

        return fetchItems(uri);
    }

    public Optional<TourApiItem> fetchDetailCommon(String contentId) {
        URI uri = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .path("/detailCommon2")
                .queryParam("serviceKey", properties.getServiceKey())
                .queryParam("contentId", contentId)
                .queryParam("MobileOS", "ETC")
                .queryParam("MobileApp", "GangwonCompanion")
                .queryParam("_type", "json")
                .build(true)
                .toUri();

        List<TourApiItem> items = fetchItems(uri);
        return items.isEmpty() ? Optional.empty() : Optional.of(items.get(0));
    }

    private List<TourApiItem> fetchItems(URI uri) {
        try {
            String body = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);

            if (body == null) return Collections.emptyList();
            return parseItems(body);

        } catch (Exception e) {
            log.error("Tour API 호출 실패: uri={}, error={}", uri, e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<TourApiItem> parseItems(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode itemsNode = root.path("response").path("body").path("items");

        if (itemsNode.isTextual() || itemsNode.isMissingNode()) {
            return Collections.emptyList();
        }

        JsonNode itemNode = itemsNode.path("item");
        if (itemNode.isMissingNode() || itemNode.isNull()) {
            return Collections.emptyList();
        }

        if (itemNode.isArray()) {
            return objectMapper.readValue(itemNode.toString(), new TypeReference<>() {});
        } else {
            TourApiItem single = objectMapper.readValue(itemNode.toString(), TourApiItem.class);
            return List.of(single);
        }
    }
}
