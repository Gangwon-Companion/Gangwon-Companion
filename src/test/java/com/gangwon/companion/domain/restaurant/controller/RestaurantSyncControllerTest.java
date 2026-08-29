package com.gangwon.companion.domain.restaurant.controller;

import com.gangwon.companion.domain.restaurant.service.RestaurantSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RestaurantSyncControllerTest {

    private MockMvc mockMvc;
    private RestaurantSyncService restaurantSyncService;

    @BeforeEach
    void setUp() {
        restaurantSyncService = mock(RestaurantSyncService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new RestaurantSyncController(restaurantSyncService)
        ).build();
    }

    @Test
    void syncRestaurants() throws Exception {
        mockMvc.perform(post("/api/v1/admin/restaurants/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("음식점 데이터 동기화가 완료되었습니다."));

        verify(restaurantSyncService).sync();
    }
}
