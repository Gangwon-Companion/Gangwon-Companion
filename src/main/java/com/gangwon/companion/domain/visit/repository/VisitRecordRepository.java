package com.gangwon.companion.domain.visit.repository;

import com.gangwon.companion.domain.visit.entity.VisitRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface VisitRecordRepository extends JpaRepository<VisitRecord, Long> {

    long countByUserUsername(String username);
    List<VisitRecord> findByUserUsernameOrderByVisitedAtDesc(String username, Pageable pageable);
}
