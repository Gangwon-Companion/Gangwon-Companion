package com.gangwon.companion.domain.travelprofile.repository;
import com.gangwon.companion.domain.travelprofile.entity.TravelProfileAnalysisJob;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
public interface TravelProfileAnalysisJobRepository extends JpaRepository<TravelProfileAnalysisJob, UUID> {
    Optional<TravelProfileAnalysisJob> findFirstByUsernameAndStatusInOrderByCreatedAtDesc(String username, Collection<TravelProfileAnalysisJob.Status> statuses);
    long deleteByStatusInAndUpdatedAtBefore(Collection<TravelProfileAnalysisJob.Status> statuses, Instant cutoff);
}
