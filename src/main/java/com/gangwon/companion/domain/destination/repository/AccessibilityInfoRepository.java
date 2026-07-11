package com.gangwon.companion.domain.destination.repository;

import com.gangwon.companion.domain.destination.entity.AccessibilityInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccessibilityInfoRepository extends JpaRepository<AccessibilityInfo, Long> {
    Optional<AccessibilityInfo> findByDestinationId(Long destinationId);

    boolean existsByDestinationIdAndContentId(Long destinationId, Long contentId);
}
