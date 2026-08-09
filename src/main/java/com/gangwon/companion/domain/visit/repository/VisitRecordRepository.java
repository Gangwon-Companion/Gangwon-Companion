package com.gangwon.companion.domain.visit.repository;

import com.gangwon.companion.domain.visit.entity.VisitRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitRecordRepository extends JpaRepository<VisitRecord, Long> {

    long countByUserUsername(String username);
}
