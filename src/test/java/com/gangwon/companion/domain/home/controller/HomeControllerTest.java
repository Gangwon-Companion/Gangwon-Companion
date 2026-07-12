package com.gangwon.companion.domain.home.controller;

import com.gangwon.companion.domain.home.client.TourApiClient;
import com.gangwon.companion.domain.home.service.HomeService;
import com.gangwon.companion.domain.home.repository.SpecialOfferRepository;
import com.gangwon.companion.domain.search.repository.SearchHistoryRepository;
import com.gangwon.companion.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HomeControllerTest {

    MockMvc mockMvc;
    UserRepository userRepository;
    SearchHistoryRepository searchHistoryRepository;
    SpecialOfferRepository specialOfferRepository;
    TourApiClient tourApiClient;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        searchHistoryRepository = mock(SearchHistoryRepository.class);
        specialOfferRepository = mock(SpecialOfferRepository.class);
        tourApiClient = mock(TourApiClient.class);
        when(userRepository.findByUsername("testuser1")).thenReturn(Optional.empty());

        mockMvc = MockMvcBuilders
                .standaloneSetup(new HomeController(new HomeService(
                        userRepository,
                        searchHistoryRepository,
                        specialOfferRepository,
                        tourApiClient
                )))
                .build();
    }

    @Test
    void getPromotionDetailsUsesAuthenticatedUserContext() throws Exception {
        mockMvc.perform(get("/api/promotions/details")
                        .param("keyword", "stay")
                        .param("region", "gangneung")
                        .principal(new TestingAuthenticationToken("testuser1", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getPromotionBannersFiltersByCategory() throws Exception {
        mockMvc.perform(get("/api/banners")
                        .param("category", "spot"))
                .andExpect(status().isOk());
    }

    @Test
    void getHotPlacesAppliesLimit() throws Exception {
        mockMvc.perform(get("/api/promotions/hotplace")
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
