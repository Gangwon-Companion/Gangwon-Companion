package com.gangwon.companion.domain.lodging.controller;

import com.gangwon.companion.domain.lodging.dto.response.LodgingDetailResponse;
import com.gangwon.companion.domain.lodging.dto.response.LodgingListResponse;
import com.gangwon.companion.domain.lodging.dto.response.LodgingReviewResponse;
import com.gangwon.companion.domain.lodging.entity.Lodging;
import com.gangwon.companion.domain.lodging.service.LodgingService;
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
class LodgingControllerTest {

    @Mock LodgingService lodgingService;
    @InjectMocks LodgingController lodgingController;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(lodgingController)
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(authenticatedUserResolver())
                .build();
    }

    @Test
    @DisplayName("GET /api/lodgings -> HTTP 200 list")
    void searchLodgings_returnsList_whenRequestValid() throws Exception {
        given(lodgingService.searchLodgings(any()))
                .willReturn(new LodgingListResponse(0, List.of()));

        mockMvc.perform(get("/api/lodgings")
                        .param("keyword", "hotel")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    @DisplayName("GET /api/lodgings/{lodgingId} -> HTTP 200 detail")
    void getLodgingDetail_returnsDetail_whenLodgingExists() throws Exception {
        given(lodgingService.getLodgingDetail(1L))
                .willReturn(new LodgingDetailResponse(lodging(), List.of()));

        mockMvc.perform(get("/api/lodgings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ocean Hotel"))
                .andExpect(jsonPath("$.location.address").value("Sokcho"));
    }

    @Test
    @DisplayName("POST /api/lodgings/{lodgingId}/reviews -> HTTP 201 created")
    void createReview_returnsCreated_whenRequestValid() throws Exception {
        given(lodgingService.createReview(eq(1L), eq("owner"), any()))
                .willReturn(new LodgingReviewResponse(10L, "ownerNick", "great", 4.5, LocalDateTime.now()));

        mockMvc.perform(post("/api/lodgings/1/reviews")
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
    @DisplayName("POST /api/lodgings/{lodgingId}/reviews invalid request -> HTTP 400 VALIDATION_FAILED")
    void createReview_returnsBadRequest_whenRequestInvalid() throws Exception {
        mockMvc.perform(post("/api/lodgings/1/reviews")
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
    @DisplayName("PUT /api/lodgings/{lodgingId}/reviews/{reviewId} not owner -> HTTP 403 REVIEW_FORBIDDEN")
    void updateReview_returnsForbidden_whenUserIsNotOwner() throws Exception {
        given(lodgingService.updateReview(eq(1L), eq(10L), eq("owner"), any()))
                .willThrow(new BusinessException(ErrorCode.REVIEW_FORBIDDEN));

        mockMvc.perform(put("/api/lodgings/1/reviews/10")
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
    @DisplayName("DELETE /api/lodgings/{lodgingId}/reviews/{reviewId} owner -> HTTP 204")
    void deleteReview_returnsNoContent_whenUserIsOwner() throws Exception {
        mockMvc.perform(delete("/api/lodgings/1/reviews/10"))
                .andExpect(status().isNoContent());

        verify(lodgingService).deleteReview(1L, 10L, "owner");
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

    private Lodging lodging() {
        return Lodging.builder()
                .name("Ocean Hotel")
                .description("description")
                .region("Sokcho")
                .price(100000L)
                .rating(4.7)
                .thumbnailUrl("https://example.com/lodging.jpg")
                .address("Sokcho")
                .latitude(38.2)
                .longitude(128.5)
                .build();
    }
}
