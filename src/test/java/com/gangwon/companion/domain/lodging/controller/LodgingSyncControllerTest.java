package com.gangwon.companion.domain.lodging.controller;

import com.gangwon.companion.domain.lodging.service.LodgingSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LodgingSyncControllerTest {

    private MockMvc mockMvc;
    private LodgingSyncService lodgingSyncService;

    @BeforeEach
    void setUp() {
        lodgingSyncService = mock(LodgingSyncService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new LodgingSyncController(lodgingSyncService)
        ).build();
    }

    @Test
    void syncLodgings() throws Exception {
        mockMvc.perform(post("/api/v1/admin/lodgings/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("숙소 데이터 동기화가 완료되었습니다."));

        verify(lodgingSyncService).sync();
    }
}
