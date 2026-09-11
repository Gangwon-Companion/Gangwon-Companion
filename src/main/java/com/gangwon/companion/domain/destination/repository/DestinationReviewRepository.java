package com.gangwon.companion.domain.destination.repository;

import com.gangwon.companion.domain.destination.entity.DestinationReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DestinationReviewRepository extends JpaRepository<DestinationReview, Long> {

    List<DestinationReview> findByDestinationId(Long destinationId);

    List<DestinationReview> findAllByUserUsername(String username);

    Optional<DestinationReview> findByIdAndDestinationId(Long id, Long destinationId);

    long countByDestinationId(Long destinationId);

    long countByUserUsername(String username);

    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM DestinationReview r WHERE r.destination.id = :destinationId")
    Double calculateAverageRatingByDestinationId(@Param("destinationId") Long destinationId);
}
