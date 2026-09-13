package com.gangwon.companion.domain.search.repository;

import com.gangwon.companion.domain.search.entity.SearchHistory;
import com.gangwon.companion.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import org.springframework.data.domain.Pageable;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

    List<SearchHistory> findTop5ByUserOrderBySearchedAtDesc(User user);
    List<SearchHistory> findByUserUsernameOrderBySearchedAtDesc(String username, Pageable pageable);
}
