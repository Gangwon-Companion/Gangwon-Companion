package com.gangwon.companion.domain.course.service;

import tools.jackson.databind.JsonNode;
import com.gangwon.companion.domain.course.client.AiTravelClient;
import com.gangwon.companion.domain.course.dto.CourseRecommendationRequest;
import com.gangwon.companion.domain.course.dto.AiCourseRecommendationRequest;
import com.gangwon.companion.domain.travelprofile.service.TravelProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CourseRecommendationService {
    private final AiTravelClient aiTravelClient;
    private final TravelProfileService travelProfileService;

    public JsonNode recommend(CourseRecommendationRequest request, String username) {
        var profile = travelProfileService.findUsableContext(username).orElse(null);
        return aiTravelClient.recommend(AiCourseRecommendationRequest.from(request, profile));
    }
}
