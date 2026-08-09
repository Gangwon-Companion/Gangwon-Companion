package com.gangwon.companion.domain.search.repository;

import com.gangwon.companion.domain.search.entity.SearchHistory;
import com.gangwon.companion.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

    List<SearchHistory> findTop5ByUserOrderBySearchedAtDesc(User user);
}
