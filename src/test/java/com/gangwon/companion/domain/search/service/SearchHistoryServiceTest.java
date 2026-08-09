package com.gangwon.companion.domain.search.service;

import com.gangwon.companion.domain.search.dto.SearchHistoryRequest;
import com.gangwon.companion.domain.search.entity.SearchHistory;
import com.gangwon.companion.domain.search.repository.SearchHistoryRepository;
import com.gangwon.companion.domain.user.entity.User;
import com.gangwon.companion.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SearchHistoryServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    SearchHistoryRepository searchHistoryRepository;

    @InjectMocks
    SearchHistoryService searchHistoryService;

    @Test
    void saveSearchHistory() {
        User user = User.builder()
                .username("testuser1")
                .password("password")
                .email("test@test.com")
                .nickname("tester")
                .build();
        SearchHistory savedSearchHistory = SearchHistory.builder()
                .user(user)
                .keyword("stay")
                .region("gangneung")
                .searchedAt(LocalDateTime.of(2026, 6, 20, 12, 0))
                .build();
        SearchHistoryRequest request = new SearchHistoryRequest();
        ReflectionTestUtils.setField(request, "keyword", "stay");
        ReflectionTestUtils.setField(request, "region", "gangneung");

        given(userRepository.findByUsername("testuser1")).willReturn(Optional.of(user));
        given(searchHistoryRepository.save(any(SearchHistory.class))).willReturn(savedSearchHistory);

        var response = searchHistoryService.saveSearchHistory("testuser1", request);

        assertThat(response.keyword()).isEqualTo("stay");
        assertThat(response.region()).isEqualTo("gangneung");
    }
}
