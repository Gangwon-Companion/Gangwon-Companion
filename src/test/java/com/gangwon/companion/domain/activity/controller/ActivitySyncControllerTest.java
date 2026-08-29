package com.gangwon.companion.domain.activity.controller;

import com.gangwon.companion.domain.activity.service.ActivitySyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ActivitySyncControllerTest {

    private MockMvc mockMvc;
    private ActivitySyncService activitySyncService;

    @BeforeEach
    void setUp() {
        activitySyncService = mock(ActivitySyncService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new ActivitySyncController(activitySyncService)
        ).build();
    }

    @Test
    void syncActivities() throws Exception {
        when(activitySyncService.syncGangwonActivities()).thenReturn(12);

        mockMvc.perform(post("/api/v1/admin/activities/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savedCount").value(12));

        verify(activitySyncService).syncGangwonActivities();
    }
}
