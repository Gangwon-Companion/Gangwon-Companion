package com.gangwon.companion.domain.lodging.repository;

import com.gangwon.companion.domain.lodging.entity.LodgingPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LodgingPhotoRepository extends JpaRepository<LodgingPhoto, Long> {
    boolean existsByLodgingIdAndSerialNum(Long lodgingId, String serialNum);
}
