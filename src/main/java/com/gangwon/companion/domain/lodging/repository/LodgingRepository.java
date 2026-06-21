package com.gangwon.companion.domain.lodging.repository;

import com.gangwon.companion.domain.lodging.entity.Lodging;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LodgingRepository extends JpaRepository<Lodging, Long>, JpaSpecificationExecutor<Lodging> {

    @Query("SELECT l FROM Lodging l LEFT JOIN FETCH l.photos WHERE l.id = :id")
    Optional<Lodging> findByIdWithPhotos(@Param("id") Long id);

    Optional<Lodging> findByExternalId(String externalId);

    boolean existsByExternalIdIsNotNull();

    // description이 null인 항목 = 상세 보완이 필요한 항목
    List<Lodging> findTop50ByExternalIdIsNotNullAndDescriptionIsNull();
}
