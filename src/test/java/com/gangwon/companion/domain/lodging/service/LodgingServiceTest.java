package com.gangwon.companion.domain.lodging.service;

import com.gangwon.companion.domain.lodging.dto.request.LodgingReviewRequest;
import com.gangwon.companion.domain.lodging.dto.request.LodgingSearchCriteria;
import com.gangwon.companion.domain.lodging.dto.response.LodgingListResponse;
import com.gangwon.companion.domain.lodging.dto.response.LodgingReviewResponse;
import com.gangwon.companion.domain.lodging.entity.Lodging;
import com.gangwon.companion.domain.lodging.entity.LodgingReview;
import com.gangwon.companion.domain.lodging.repository.LodgingRepository;
import com.gangwon.companion.domain.lodging.repository.LodgingReviewRepository;
import com.gangwon.companion.domain.user.entity.User;
import com.gangwon.companion.domain.user.repository.UserRepository;
import com.gangwon.companion.global.exception.BusinessException;
import com.gangwon.companion.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LodgingServiceTest {

    @Mock LodgingRepository lodgingRepository;
    @Mock LodgingReviewRepository lodgingReviewRepository;
    @Mock UserRepository userRepository;

    @InjectMocks LodgingService lodgingService;

    @Test
    @DisplayName("searchLodgings valid criteria -> list response")
    void searchLodgings_returnsListResponse_whenCriteriaValid() {
        Lodging lodging = lodging();
        given(lodgingRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(lodging)));

        LodgingListResponse response = lodgingService.searchLodgings(
                new LodgingSearchCriteria("hotel", "Sokcho", 10000L, 200000L, 4.0, 0, 20)
        );

        assertThat(response.getTotalCount()).isEqualTo(1);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getName()).isEqualTo("Ocean Hotel");
    }

    @Test
    @DisplayName("create lodging review valid user -> saved review")
    void createReview_returnsSavedReview_whenUserExists() {
        Lodging lodging = lodging();
        User user = user("owner");
        given(lodgingRepository.findById(1L)).willReturn(Optional.of(lodging));
        given(userRepository.findByUsername("owner")).willReturn(Optional.of(user));
        given(lodgingReviewRepository.save(any(LodgingReview.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        LodgingReviewResponse response = lodgingService.createReview(
                1L,
                "owner",
                new LodgingReviewRequest("great stay", 4.5)
        );

        assertThat(response.nickname()).isEqualTo("ownerNick");
        assertThat(response.content()).isEqualTo("great stay");
        assertThat(response.rating()).isEqualTo(4.5);
        verify(lodgingReviewRepository).save(any(LodgingReview.class));
    }

    @Test
    @DisplayName("update lodging review owner -> updated review")
    void updateReview_returnsUpdatedReview_whenUserIsOwner() {
        LodgingReview review = review("owner", "before", 3.0);
        given(lodgingReviewRepository.findByIdAndLodgingId(10L, 1L)).willReturn(Optional.of(review));

        LodgingReviewResponse response = lodgingService.updateReview(
                1L,
                10L,
                "owner",
                new LodgingReviewRequest("after", 5.0)
        );

        assertThat(response.content()).isEqualTo("after");
        assertThat(response.rating()).isEqualTo(5.0);
    }

    @Test
    @DisplayName("update lodging review not owner -> REVIEW_FORBIDDEN exception")
    void updateReview_throwsForbidden_whenUserIsNotOwner() {
        LodgingReview review = review("owner", "before", 3.0);
        given(lodgingReviewRepository.findByIdAndLodgingId(10L, 1L)).willReturn(Optional.of(review));

        assertThatThrownBy(() -> lodgingService.updateReview(
                1L,
                10L,
                "other",
                new LodgingReviewRequest("after", 5.0)
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.REVIEW_FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("delete lodging review owner -> review deleted")
    void deleteReview_deletesReview_whenUserIsOwner() {
        LodgingReview review = review("owner", "content", 4.0);
        given(lodgingReviewRepository.findByIdAndLodgingId(10L, 1L)).willReturn(Optional.of(review));

        lodgingService.deleteReview(1L, 10L, "owner");

        verify(lodgingReviewRepository).delete(eq(review));
    }

    @Test
    @DisplayName("delete lodging review not owner -> REVIEW_FORBIDDEN exception")
    void deleteReview_throwsForbidden_whenUserIsNotOwner() {
        LodgingReview review = review("owner", "content", 4.0);
        given(lodgingReviewRepository.findByIdAndLodgingId(10L, 1L)).willReturn(Optional.of(review));

        assertThatThrownBy(() -> lodgingService.deleteReview(1L, 10L, "other"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.REVIEW_FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("create lodging review missing lodging -> RESOURCE_NOT_FOUND exception")
    void createReview_throwsNotFound_whenLodgingDoesNotExist() {
        given(lodgingRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> lodgingService.createReview(
                1L,
                "owner",
                new LodgingReviewRequest("content", 4.0)
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.RESOURCE_NOT_FOUND.getMessage());
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

    private LodgingReview review(String username, String content, Double rating) {
        return LodgingReview.builder()
                .lodging(lodging())
                .user(user(username))
                .content(content)
                .rating(rating)
                .build();
    }

    private User user(String username) {
        return User.builder()
                .username(username)
                .password("encoded")
                .email(username + "@test.com")
                .nickname(username + "Nick")
                .build();
    }
}
