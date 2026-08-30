package com.gangwon.companion.domain.user.controller;

import com.gangwon.companion.domain.community.dto.CommunityDtos.LikedCommentSummary;
import com.gangwon.companion.domain.community.dto.CommunityDtos.MyCommentSummary;
import com.gangwon.companion.domain.community.dto.CommunityDtos.PostSummary;
import com.gangwon.companion.domain.community.service.CommunityService;
import com.gangwon.companion.domain.user.dto.response.MyPageResponse;
import com.gangwon.companion.domain.user.dto.response.MyReviewResponse;
import com.gangwon.companion.domain.user.service.UserService;
import com.gangwon.companion.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MyPageControllerTest {

    @Mock UserService userService;
    @Mock CommunityService communityService;
    @InjectMocks MyPageController myPageController;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(myPageController)
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(authenticatedUserResolver(), new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/users/me -> HTTP 200 with profile image")
    void getMyPage_returnsProfile() throws Exception {
        given(userService.getMyPage("owner")).willReturn(new MyPageResponse(
                "owner", "owner@test.com", "own", "https://example.com/profile.jpg",
                LocalDateTime.now(), new MyPageResponse.TravelStats(1, 2, 3)
        ));

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("owner"))
                .andExpect(jsonPath("$.profileImageUrl").value("https://example.com/profile.jpg"))
                .andExpect(jsonPath("$.travelStats.reviewCount").value(3));
    }

    @Test
    @DisplayName("PATCH /api/v1/users/me/profile-image -> HTTP 200")
    void changeProfileImage_returnsOk() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me/profile-image")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profileImageS3Key": "profiles/owner.jpg"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("프로필 이미지가 변경되었습니다."));

        verify(userService).changeProfileImage(eq("owner"), any());
    }

    @Test
    @DisplayName("GET /api/v1/users/me/community/liked-posts -> HTTP 200")
    void getLikedCommunityPosts_returnsPage() throws Exception {
        given(communityService.likedPosts(eq("owner"), any()))
                .willReturn(new PageImpl<>(List.of(postSummary(true, false)), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/users/me/community/liked-posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].authorProfileImageUrl").value("https://example.com/author.jpg"))
                .andExpect(jsonPath("$.content[0].liked").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/users/me/community/saved-posts -> HTTP 200")
    void getSavedCommunityPosts_returnsPage() throws Exception {
        given(communityService.savedPosts(eq("owner"), any()))
                .willReturn(new PageImpl<>(List.of(postSummary(false, true)), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/users/me/community/saved-posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].saved").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/users/me/community/liked-comments -> HTTP 200")
    void getLikedCommunityComments_returnsPage() throws Exception {
        given(communityService.likedComments(eq("owner"), any()))
                .willReturn(new PageImpl<>(List.of(new LikedCommentSummary(
                        10L, 1L, "title", "post content", "own", "own",
                        "https://example.com/author.jpg", "https://example.com/author.jpg",
                        "comment", 2, true, true, LocalDateTime.now()
                )), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/users/me/community/liked-comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].commentId").value(10))
                .andExpect(jsonPath("$.content[0].postId").value(1))
                .andExpect(jsonPath("$.content[0].liked").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/users/me/community/comments -> HTTP 200")
    void getMyCommunityComments_returnsPage() throws Exception {
        given(communityService.myComments(eq("owner"), any()))
                .willReturn(new PageImpl<>(List.of(new MyCommentSummary(
                        10L, 1L, "title", "post content", "comment", 2, LocalDateTime.now()
                )), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/users/me/community/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].commentId").value(10))
                .andExpect(jsonPath("$.content[0].postId").value(1))
                .andExpect(jsonPath("$.content[0].content").value("comment"));
    }

    @Test
    @DisplayName("GET /api/v1/users/me/reviews -> HTTP 200")
    void getMyReviews_returnsReviews() throws Exception {
        given(userService.getMyReviews("owner"))
                .willReturn(List.of(new MyReviewResponse("DESTINATION", 10L, "강릉", 5L, "좋아요", 4.5, LocalDateTime.now())));

        mockMvc.perform(get("/api/v1/users/me/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].placeType").value("DESTINATION"))
                .andExpect(jsonPath("$[0].reviewId").value(5));
    }

    @Test
    @DisplayName("DELETE /api/v1/users/me -> HTTP 200")
    void withdraw_returnsOk() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me")
                        .header("Authorization", "Bearer mock.jwt.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("회원탈퇴가 완료되었습니다."));

        verify(userService).withdraw("owner", "mock.jwt.token");
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

    private PostSummary postSummary(boolean liked, boolean saved) {
        return new PostSummary(1L, 1L, "title", "content", "own", "own",
                "https://example.com/author.jpg", "https://example.com/author.jpg",
                true, liked, saved, 3, 2, 1, 1, 1, List.of("https://example.com/post.jpg"),
                null, Set.of("tag"), LocalDateTime.now());
    }
}
