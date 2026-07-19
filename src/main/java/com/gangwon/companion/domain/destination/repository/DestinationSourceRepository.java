package com.gangwon.companion.domain.destination.repository;

import com.gangwon.companion.domain.destination.entity.Destination;
import com.gangwon.companion.domain.destination.entity.DestinationSource;
import com.gangwon.companion.domain.destination.entity.SourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DestinationSourceRepository extends JpaRepository<DestinationSource, Long> {
    boolean existsBySourceTypeAndContentId(SourceType sourceType, Long contentId);

    List<DestinationSource> findBySourceType(SourceType sourceType);
}
