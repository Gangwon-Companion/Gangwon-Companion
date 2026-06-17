package com.gangwon.companion.domain.lodging.repository;

import com.gangwon.companion.domain.lodging.entity.LodgingReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LodgingReviewRepository extends JpaRepository<LodgingReview, Long> {

    List<LodgingReview> findByLodgingId(Long lodgingId);
}
