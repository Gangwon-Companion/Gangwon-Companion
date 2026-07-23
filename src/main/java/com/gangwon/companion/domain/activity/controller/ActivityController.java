package com.gangwon.companion.domain.activity.controller;

import com.gangwon.companion.domain.activity.dto.ActivityDetailResponse;
import com.gangwon.companion.domain.activity.dto.ActivityListResponse;
import com.gangwon.companion.domain.activity.entity.ActivityCategory;
import com.gangwon.companion.domain.activity.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping
    public ResponseEntity<ActivityListResponse> getActivities(
            @RequestParam(required = false) ActivityCategory category,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(activityService.getActivities(category, region, keyword, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ActivityDetailResponse> getActivity(@PathVariable Long id) {
        return ResponseEntity.ok(activityService.getActivity(id));
    }
}
