package com.gangwon.companion.domain.activity.controller;

import com.gangwon.companion.domain.activity.dto.ActivityDetailResponse;
import com.gangwon.companion.domain.activity.dto.ActivityListResponse;
import com.gangwon.companion.domain.activity.dto.ActivitySummaryResponse;
import com.gangwon.companion.domain.activity.entity.ActivityCategory;
import com.gangwon.companion.domain.activity.service.ActivityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ActivityControllerTest {

    MockMvc mockMvc;
    ActivityService activityService;

    @BeforeEach
    void setUp() {
        activityService = mock(ActivityService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ActivityController(activityService)).build();
    }

    @Test
    void getActivitiesAppliesFiltersAndPagination() throws Exception {
        ActivitySummaryResponse item = new ActivitySummaryResponse(
                1L,
                100L,
                "양양 서핑",
                ActivityCategory.LEISURE,
                "양양군",
                "강원특별자치도 양양군",
                "https://example.com/image.jpg",
                "https://example.com/thumbnail.jpg",
                38.0,
                128.0
        );
        when(activityService.getActivities(ActivityCategory.LEISURE, "양양", "서핑", 0, 10))
                .thenReturn(new ActivityListResponse(List.of(item), 0, 10, 1, 1));

        mockMvc.perform(get("/api/activities")
                        .param("category", "LEISURE")
                        .param("region", "양양")
                        .param("keyword", "서핑")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].category").value("LEISURE"));

        verify(activityService).getActivities(ActivityCategory.LEISURE, "양양", "서핑", 0, 10);
    }

    @Test
    void getActivityReturnsDetail() throws Exception {
        when(activityService.getActivity(1L)).thenReturn(new ActivityDetailResponse(
                1L,
                100L,
                "양양 서핑",
                ActivityCategory.LEISURE,
                "양양군",
                "강원특별자치도 양양군",
                "https://example.com/image.jpg",
                "033-000-0000",
                "https://example.com",
                "서핑 체험",
                "09:00~18:00",
                "연중무휴",
                "유료",
                "주차 가능",
                "https://example.com/reservation",
                38.0,
                128.0
        ));

        mockMvc.perform(get("/api/activities/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.overview").value("서핑 체험"));
    }
}
