package com.gangwon.companion.domain.course.service;

import tools.jackson.databind.JsonNode;
import com.gangwon.companion.domain.course.client.AiTravelClient;
import com.gangwon.companion.domain.course.dto.CourseRecommendationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CourseRecommendationService {
    private final AiTravelClient aiTravelClient;

    public JsonNode recommend(CourseRecommendationRequest request) {
        return aiTravelClient.recommend(request);
    }
}
