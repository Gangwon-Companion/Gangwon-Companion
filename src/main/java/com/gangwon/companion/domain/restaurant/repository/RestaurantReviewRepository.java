package com.gangwon.companion.domain.restaurant.repository;

import com.gangwon.companion.domain.restaurant.entity.RestaurantReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RestaurantReviewRepository extends JpaRepository<RestaurantReview, Long> {

    List<RestaurantReview> findByRestaurantId(Long restaurantId);

    List<RestaurantReview> findAllByUserUsername(String username);

    Optional<RestaurantReview> findByIdAndRestaurantId(Long id, Long restaurantId);

    long countByRestaurantId(Long restaurantId);

    long countByUserUsername(String username);

    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM RestaurantReview r WHERE r.restaurant.id = :restaurantId")
    Double calculateAverageRatingByRestaurantId(@Param("restaurantId") Long restaurantId);
}
