package com.gangwon.companion.domain.destination.repository;

import com.gangwon.companion.domain.destination.entity.PetInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface PetInfoRepository extends JpaRepository<PetInfo, Long> {
    Optional<PetInfo> findByDestinationId(Long destinationId);

    boolean existsByDestinationIdAndContentId(Long destinationId, Long contentId);

    List<PetInfo> findAllByDestinationIdIn(List<Long> destinationIds);
}
