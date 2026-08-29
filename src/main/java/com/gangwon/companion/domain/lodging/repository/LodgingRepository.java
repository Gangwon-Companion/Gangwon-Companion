package com.gangwon.companion.domain.lodging.repository;

import com.gangwon.companion.domain.lodging.entity.Lodging;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LodgingRepository extends JpaRepository<Lodging, Long>, JpaSpecificationExecutor<Lodging> {

    @Query("SELECT l FROM Lodging l LEFT JOIN FETCH l.photos WHERE l.id = :id")
    Optional<Lodging> findByIdWithPhotos(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Lodging l WHERE l.id = :id")
    Optional<Lodging> findByIdForUpdate(@Param("id") Long id);

    Optional<Lodging> findByExternalId(String externalId);

    boolean existsByExternalIdIsNotNull();

    @Query("""
            SELECT DISTINCT l FROM Lodging l
            LEFT JOIN l.photos p
            WHERE l.externalId IS NOT NULL
              AND (l.description IS NULL OR l.roomType IS NULL OR p.id IS NULL)
            ORDER BY l.id
            """)
    List<Lodging> findNeedingDetails(Pageable pageable);
}
