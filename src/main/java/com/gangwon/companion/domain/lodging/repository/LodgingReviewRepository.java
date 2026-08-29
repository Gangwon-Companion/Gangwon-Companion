package com.gangwon.companion.domain.lodging.repository;

import com.gangwon.companion.domain.lodging.entity.LodgingReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LodgingReviewRepository extends JpaRepository<LodgingReview, Long> {

    List<LodgingReview> findByLodgingId(Long lodgingId);

    Optional<LodgingReview> findByIdAndLodgingId(Long id, Long lodgingId);

    long countByLodgingId(Long lodgingId);

    long countByUserUsername(String username);

    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM LodgingReview r WHERE r.lodging.id = :lodgingId")
    Double calculateAverageRatingByLodgingId(@Param("lodgingId") Long lodgingId);
}
