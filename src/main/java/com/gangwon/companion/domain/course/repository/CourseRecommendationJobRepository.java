package com.gangwon.companion.domain.course.repository;

import com.gangwon.companion.domain.course.entity.CourseRecommendationJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

public interface CourseRecommendationJobRepository extends JpaRepository<CourseRecommendationJob, UUID> {
    long deleteByStatusInAndUpdatedAtBefore(Collection<CourseRecommendationJob.Status> statuses, Instant cutoff);
}
