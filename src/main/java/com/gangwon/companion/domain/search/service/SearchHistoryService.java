package com.gangwon.companion.domain.search.service;

import com.gangwon.companion.domain.search.dto.SearchHistoryRequest;
import com.gangwon.companion.domain.search.dto.SearchHistoryResponse;
import com.gangwon.companion.domain.search.entity.SearchHistory;
import com.gangwon.companion.domain.search.repository.SearchHistoryRepository;
import com.gangwon.companion.domain.user.entity.User;
import com.gangwon.companion.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchHistoryService {

    private final UserRepository userRepository;
    private final SearchHistoryRepository searchHistoryRepository;

    @Transactional
    public SearchHistoryResponse saveSearchHistory(String username, SearchHistoryRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        SearchHistory searchHistory = SearchHistory.builder()
                .user(user)
                .keyword(request.getKeyword())
                .region(request.getRegion())
                .build();

        SearchHistory savedSearchHistory = searchHistoryRepository.save(searchHistory);

        return new SearchHistoryResponse(
                savedSearchHistory.getId(),
                savedSearchHistory.getKeyword(),
                savedSearchHistory.getRegion(),
                savedSearchHistory.getSearchedAt()
        );
    }
}
