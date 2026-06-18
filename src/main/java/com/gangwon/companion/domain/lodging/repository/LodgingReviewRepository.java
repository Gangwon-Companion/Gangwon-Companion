package com.gangwon.companion.domain.lodging.repository;

import com.gangwon.companion.domain.lodging.entity.LodgingReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LodgingReviewRepository extends JpaRepository<LodgingReview, Long> {

    List<LodgingReview> findByLodgingId(Long lodgingId);

    Optional<LodgingReview> findByIdAndLodgingId(Long id, Long lodgingId);
}
