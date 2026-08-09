package com.gangwon.companion.domain.destination.repository;

import com.gangwon.companion.domain.destination.entity.DestinationDetail;
import com.gangwon.companion.domain.destination.entity.SourceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DestinationDetailRepository extends JpaRepository<DestinationDetail,Long> {
    Optional<DestinationDetail> findByDestinationIdAndSourceType(Long destinationId, SourceType sourceType);

    boolean existsBySourceTypeAndContentId(SourceType sourceType, Long contentId);
}
