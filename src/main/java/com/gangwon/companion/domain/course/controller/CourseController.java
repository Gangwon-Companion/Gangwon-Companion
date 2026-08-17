package com.gangwon.companion.domain.course.controller;

import com.gangwon.companion.domain.course.dto.CourseResponse;
import com.gangwon.companion.domain.course.repository.SavedCourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {
    private final SavedCourseRepository repository;

    @GetMapping
    public List<CourseResponse> getMyCourses(Authentication authentication) {
        return repository.findAllByUserUsernameOrderByCreatedAtDesc(authentication.getName()).stream().map(CourseResponse::from).toList();
    }
}
