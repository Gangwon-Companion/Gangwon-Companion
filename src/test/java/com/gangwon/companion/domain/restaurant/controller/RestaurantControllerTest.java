package com.gangwon.companion.domain.restaurant.controller;

import com.gangwon.companion.domain.restaurant.dto.response.RestaurantDetailResponse;
import com.gangwon.companion.domain.restaurant.dto.response.RestaurantListResponse;
import com.gangwon.companion.domain.restaurant.dto.response.RestaurantReviewResponse;
import com.gangwon.companion.domain.restaurant.entity.Restaurant;
import com.gangwon.companion.domain.restaurant.service.RestaurantService;
import com.gangwon.companion.global.exception.BusinessException;
import com.gangwon.companion.global.exception.ErrorCode;
import com.gangwon.companion.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RestaurantControllerTest {

    @Mock RestaurantService restaurantService;
    @InjectMocks RestaurantController restaurantController;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(restaurantController)
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(authenticatedUserResolver())
                .build();
    }

    @Test
    @DisplayName("GET /api/restaurants -> HTTP 200 list")
    void searchRestaurants_returnsList_whenRequestValid() throws Exception {
        given(restaurantService.searchRestaurants(any()))
                .willReturn(new RestaurantListResponse(0, List.of()));

        mockMvc.perform(get("/api/restaurants")
                        .param("keyword", "food")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    @DisplayName("GET /api/restaurants/{restaurantId} -> HTTP 200 detail")
    void getRestaurantDetail_returnsDetail_whenRestaurantExists() throws Exception {
        given(restaurantService.getRestaurantDetail(1L))
                .willReturn(new RestaurantDetailResponse(restaurant(), List.of()));

        mockMvc.perform(get("/api/restaurants/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sea Restaurant"))
                .andExpect(jsonPath("$.address").value("Gangneung"));
    }

    @Test
    @DisplayName("POST /api/restaurants/{restaurantId}/reviews -> HTTP 201 created")
    void createReview_returnsCreated_whenRequestValid() throws Exception {
        given(restaurantService.createReview(eq(1L), eq("owner"), any()))
                .willReturn(new RestaurantReviewResponse(10L, "ownerNick", "great", 4.5, LocalDateTime.now()));

        mockMvc.perform(post("/api/restaurants/1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "great",
                                  "rating": 4.5
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reviewId").value(10))
                .andExpect(jsonPath("$.content").value("great"));
    }

    @Test
    @DisplayName("POST /api/restaurants/{restaurantId}/reviews invalid request -> HTTP 400 VALIDATION_FAILED")
    void createReview_returnsBadRequest_whenRequestInvalid() throws Exception {
        mockMvc.perform(post("/api/restaurants/1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "",
                                  "rating": 6.0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_FAILED.getCode()));
    }

    @Test
    @DisplayName("PUT /api/restaurants/{restaurantId}/reviews/{reviewId} not owner -> HTTP 403 REVIEW_FORBIDDEN")
    void updateReview_returnsForbidden_whenUserIsNotOwner() throws Exception {
        given(restaurantService.updateReview(eq(1L), eq(10L), eq("owner"), any()))
                .willThrow(new BusinessException(ErrorCode.REVIEW_FORBIDDEN));

        mockMvc.perform(put("/api/restaurants/1/reviews/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "updated",
                                  "rating": 4.0
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.REVIEW_FORBIDDEN.getCode()));
    }

    @Test
    @DisplayName("DELETE /api/restaurants/{restaurantId}/reviews/{reviewId} owner -> HTTP 204")
    void deleteReview_returnsNoContent_whenUserIsOwner() throws Exception {
        mockMvc.perform(delete("/api/restaurants/1/reviews/10"))
                .andExpect(status().isNoContent());

        verify(restaurantService).deleteReview(1L, 10L, "owner");
    }

    private HandlerMethodArgumentResolver authenticatedUserResolver() {
        return new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return User.withUsername("owner").password("password").roles("USER").build();
            }
        };
    }

    private Restaurant restaurant() {
        return Restaurant.builder()
                .name("Sea Restaurant")
                .menuType("Korean")
                .region("Gangneung")
                .rating(4.6)
                .thumbnailUrl("https://example.com/restaurant.jpg")
                .address("Gangneung")
                .latitude(37.7)
                .longitude(128.9)
                .build();
    }
}
