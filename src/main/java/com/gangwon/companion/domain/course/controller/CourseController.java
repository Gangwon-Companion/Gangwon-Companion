package com.gangwon.companion.domain.course.controller;

import com.gangwon.companion.domain.course.dto.CourseResponse;
import com.gangwon.companion.domain.course.dto.CourseRecommendationRequest;
import com.gangwon.companion.domain.course.dto.CourseSaveRequest;
import com.gangwon.companion.domain.course.dto.CourseRecommendationJobResponse;
import com.gangwon.companion.domain.course.dto.CourseRecommendationJobSubmittedResponse;
import com.gangwon.companion.domain.course.repository.SavedCourseRepository;
import com.gangwon.companion.domain.course.service.CourseRecommendationJobService;
import com.gangwon.companion.domain.course.service.CourseRecommendationService;
import com.gangwon.companion.domain.course.service.CourseService;
import tools.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {
    private final SavedCourseRepository repository;
    private final CourseRecommendationService recommendationService;
    private final CourseRecommendationJobService recommendationJobService;
    private final CourseService courseService;

    @PostMapping("/recommendations")
    public JsonNode recommend(@Valid @RequestBody CourseRecommendationRequest request) {
        return recommendationService.recommend(request);
    }

    @PostMapping("/recommendations/jobs")
    public ResponseEntity<CourseRecommendationJobSubmittedResponse> submitRecommendationJob(
            @Valid @RequestBody CourseRecommendationRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(recommendationJobService.submit(request, authentication.getName()));
    }

    @GetMapping("/recommendations/jobs/{jobId}")
    public CourseRecommendationJobResponse getRecommendationJob(
            @PathVariable UUID jobId,
            Authentication authentication
    ) {
        return recommendationJobService.get(jobId, authentication.getName());
    }

    @GetMapping
    public List<CourseResponse> getMyCourses(Authentication authentication) {
        return repository.findAllByUserUsernameOrderByCreatedAtDesc(authentication.getName()).stream().map(CourseResponse::from).toList();
    }

    @PostMapping
    public CourseResponse saveCourse(
            Authentication authentication,
            @Valid @RequestBody CourseSaveRequest request
    ) {
        return courseService.save(authentication.getName(), request);
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long courseId, Authentication authentication) {
        courseService.delete(authentication.getName(), courseId);
        return ResponseEntity.noContent().build();
    }
}
