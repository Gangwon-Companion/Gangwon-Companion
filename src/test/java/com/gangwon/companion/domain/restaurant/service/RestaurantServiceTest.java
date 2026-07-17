package com.gangwon.companion.domain.restaurant.service;

import com.gangwon.companion.domain.restaurant.dto.request.RestaurantReviewRequest;
import com.gangwon.companion.domain.restaurant.dto.request.RestaurantSearchCriteria;
import com.gangwon.companion.domain.restaurant.dto.response.RestaurantListResponse;
import com.gangwon.companion.domain.restaurant.dto.response.RestaurantReviewResponse;
import com.gangwon.companion.domain.restaurant.entity.Restaurant;
import com.gangwon.companion.domain.restaurant.entity.RestaurantReview;
import com.gangwon.companion.domain.restaurant.repository.RestaurantRepository;
import com.gangwon.companion.domain.restaurant.repository.RestaurantReviewRepository;
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
class RestaurantServiceTest {

    @Mock RestaurantRepository restaurantRepository;
    @Mock RestaurantReviewRepository restaurantReviewRepository;
    @Mock UserRepository userRepository;

    @InjectMocks RestaurantService restaurantService;

    @Test
    @DisplayName("searchRestaurants valid criteria -> list response")
    void searchRestaurants_returnsListResponse_whenCriteriaValid() {
        Restaurant restaurant = restaurant();
        given(restaurantRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(restaurant)));

        RestaurantListResponse response = restaurantService.searchRestaurants(
                new RestaurantSearchCriteria("noodle", "Korean", "Gangneung", 0, 20)
        );

        assertThat(response.getTotalCount()).isEqualTo(1);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getName()).isEqualTo("Sea Restaurant");
    }

    @Test
    @DisplayName("create restaurant review valid user -> saved review")
    void createReview_returnsSavedReview_whenUserExists() {
        Restaurant restaurant = restaurant();
        User user = user("owner");
        given(restaurantRepository.findById(1L)).willReturn(Optional.of(restaurant));
        given(userRepository.findByUsername("owner")).willReturn(Optional.of(user));
        given(restaurantReviewRepository.save(any(RestaurantReview.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        RestaurantReviewResponse response = restaurantService.createReview(
                1L,
                "owner",
                new RestaurantReviewRequest("great food", 4.5)
        );

        assertThat(response.nickname()).isEqualTo("ownerNick");
        assertThat(response.content()).isEqualTo("great food");
        assertThat(response.rating()).isEqualTo(4.5);
        verify(restaurantReviewRepository).save(any(RestaurantReview.class));
    }

    @Test
    @DisplayName("update restaurant review owner -> updated review")
    void updateReview_returnsUpdatedReview_whenUserIsOwner() {
        RestaurantReview review = review("owner", "before", 3.0);
        given(restaurantReviewRepository.findByIdAndRestaurantId(10L, 1L)).willReturn(Optional.of(review));

        RestaurantReviewResponse response = restaurantService.updateReview(
                1L,
                10L,
                "owner",
                new RestaurantReviewRequest("after", 5.0)
        );

        assertThat(response.content()).isEqualTo("after");
        assertThat(response.rating()).isEqualTo(5.0);
    }

    @Test
    @DisplayName("update restaurant review not owner -> REVIEW_FORBIDDEN exception")
    void updateReview_throwsForbidden_whenUserIsNotOwner() {
        RestaurantReview review = review("owner", "before", 3.0);
        given(restaurantReviewRepository.findByIdAndRestaurantId(10L, 1L)).willReturn(Optional.of(review));

        assertThatThrownBy(() -> restaurantService.updateReview(
                1L,
                10L,
                "other",
                new RestaurantReviewRequest("after", 5.0)
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.REVIEW_FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("delete restaurant review owner -> review deleted")
    void deleteReview_deletesReview_whenUserIsOwner() {
        RestaurantReview review = review("owner", "content", 4.0);
        given(restaurantReviewRepository.findByIdAndRestaurantId(10L, 1L)).willReturn(Optional.of(review));

        restaurantService.deleteReview(1L, 10L, "owner");

        verify(restaurantReviewRepository).delete(eq(review));
    }

    @Test
    @DisplayName("delete restaurant review not owner -> REVIEW_FORBIDDEN exception")
    void deleteReview_throwsForbidden_whenUserIsNotOwner() {
        RestaurantReview review = review("owner", "content", 4.0);
        given(restaurantReviewRepository.findByIdAndRestaurantId(10L, 1L)).willReturn(Optional.of(review));

        assertThatThrownBy(() -> restaurantService.deleteReview(1L, 10L, "other"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.REVIEW_FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("create restaurant review missing restaurant -> RESOURCE_NOT_FOUND exception")
    void createReview_throwsNotFound_whenRestaurantDoesNotExist() {
        given(restaurantRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> restaurantService.createReview(
                1L,
                "owner",
                new RestaurantReviewRequest("content", 4.0)
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.RESOURCE_NOT_FOUND.getMessage());
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

    private RestaurantReview review(String username, String content, Double rating) {
        return RestaurantReview.builder()
                .restaurant(restaurant())
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
