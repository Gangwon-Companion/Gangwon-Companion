package com.gangwon.companion.domain.course.client;

import tools.jackson.databind.JsonNode;
import com.gangwon.companion.domain.course.dto.CourseRecommendationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AiTravelClient {
    private final RestClient client;

    public AiTravelClient(@Value("${ai.base-url}") String baseUrl) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
    }

    public JsonNode recommend(CourseRecommendationRequest request) {
        return client.post()
                .uri("/internal/travel/plan")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(JsonNode.class);
    }
}
