package com.gangwon.companion.domain.restaurant.repository;

import com.gangwon.companion.domain.restaurant.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long>, JpaSpecificationExecutor<Restaurant> {

    @Query("SELECT r FROM Restaurant r LEFT JOIN FETCH r.photos WHERE r.id = :id")
    Optional<Restaurant> findByIdWithPhotos(@Param("id") Long id);
}
