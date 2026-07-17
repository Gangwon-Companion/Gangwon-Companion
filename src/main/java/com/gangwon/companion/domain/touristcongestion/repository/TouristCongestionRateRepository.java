package com.gangwon.companion.domain.touristcongestion.repository;

import com.gangwon.companion.domain.touristcongestion.entity.TouristCongestionRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface TouristCongestionRateRepository extends JpaRepository<TouristCongestionRate, Long>,
        JpaSpecificationExecutor<TouristCongestionRate> {
    Optional<TouristCongestionRate> findByExternalKey(String externalKey);
}
