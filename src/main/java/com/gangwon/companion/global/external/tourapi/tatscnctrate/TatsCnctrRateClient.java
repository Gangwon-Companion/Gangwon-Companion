package com.gangwon.companion.global.external.tourapi.tatscnctrate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gangwon.companion.global.config.TatsCnctrRateProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class TatsCnctrRateClient {

    private static final String MOBILE_OS = "ETC";
    private static final String MOBILE_APP = "GangwonCompanion";

    private final TatsCnctrRateProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public TatsCnctrRateClient(TatsCnctrRateProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.create();
        this.objectMapper = new ObjectMapper();
    }

    public List<JsonNode> fetchRates(String areaCd, String signguCd, int pageNo, int numOfRows) {
        URI uri = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .path("/tatsCnctrRatedList")
                .queryParam("serviceKey", properties.getServiceKey())
                .queryParam("numOfRows", numOfRows)
                .queryParam("pageNo", pageNo)
                .queryParam("MobileOS", MOBILE_OS)
                .queryParam("MobileApp", MOBILE_APP)
                .queryParam("areaCd", areaCd)
                .queryParam("signguCd", signguCd)
                .queryParam("_type", "json")
                .build(true)
                .toUri();

        return fetchItems(uri);
    }

    private List<JsonNode> fetchItems(URI uri) {
        try {
            String body = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);

            if (body == null) {
                return Collections.emptyList();
            }
            return parseItems(body);
        } catch (Exception e) {
            log.error("관광혼잡도 API 데이터 조회 실패: uri={}, error={}", uri, e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<JsonNode> parseItems(String body) throws Exception {
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
        }

        return List.of(itemNode);
    }
}
