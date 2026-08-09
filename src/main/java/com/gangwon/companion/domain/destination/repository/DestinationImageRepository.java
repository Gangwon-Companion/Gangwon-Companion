package com.gangwon.companion.domain.destination.repository;

import com.gangwon.companion.domain.destination.entity.DestinationImage;
import com.gangwon.companion.domain.destination.entity.SourceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DestinationImageRepository extends JpaRepository<DestinationImage, Long> {
    List<DestinationImage> findByDestinationIdAndSourceType(Long destinationId, SourceType sourceType);

    boolean existsBySourceTypeAndContentIdAndSerialNum(SourceType sourceType, Long contentId, String serialNum);
}
