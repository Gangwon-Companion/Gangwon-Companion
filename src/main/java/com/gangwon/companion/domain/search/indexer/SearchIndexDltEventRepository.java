package com.gangwon.companion.domain.search.indexer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SearchIndexDltEventRepository extends JpaRepository<SearchIndexDltEvent, Long> {
    Optional<SearchIndexDltEvent> findByDomainAndPlaceId(String domain, long placeId);

    List<SearchIndexDltEvent> findTop100ByOrderByCreatedAtAsc();
}
