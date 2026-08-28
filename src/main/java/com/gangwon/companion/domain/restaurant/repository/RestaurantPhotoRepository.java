package com.gangwon.companion.domain.restaurant.repository;

import com.gangwon.companion.domain.restaurant.entity.RestaurantPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantPhotoRepository extends JpaRepository<RestaurantPhoto, Long> {
    boolean existsByRestaurantIdAndSerialNum(Long restaurantId, String serialNum);
}
