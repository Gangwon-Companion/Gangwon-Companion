package com.gangwon.companion.domain.travelprofile.repository;
import com.gangwon.companion.domain.travelprofile.entity.TravelProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface TravelProfileRepository extends JpaRepository<TravelProfile, Long> {
    Optional<TravelProfile> findByUserUsername(String username);
}
