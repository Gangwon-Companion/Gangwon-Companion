package com.gangwon.companion.domain.restaurant.repository;

import com.gangwon.companion.domain.restaurant.entity.Restaurant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long>, JpaSpecificationExecutor<Restaurant> {

    @Query("SELECT r FROM Restaurant r LEFT JOIN FETCH r.photos WHERE r.id = :id")
    Optional<Restaurant> findByIdWithPhotos(@Param("id") Long id);

    Optional<Restaurant> findByExternalId(String externalId);

    boolean existsByExternalIdIsNotNull();

    @Query("""
            SELECT DISTINCT r FROM Restaurant r
            LEFT JOIN r.photos p
            WHERE r.externalId IS NOT NULL
              AND (r.firstMenu IS NULL OR p.id IS NULL)
            ORDER BY r.id
            """)
    List<Restaurant> findNeedingDetails(Pageable pageable);
}
