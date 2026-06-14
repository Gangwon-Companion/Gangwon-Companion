package com.gangwon.companion.domain.restaurant.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "restaurant_photos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RestaurantPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(nullable = false)
    private String url;

    @Builder
    public RestaurantPhoto(Restaurant restaurant, String url) {
        this.restaurant = restaurant;
        this.url = url;
    }
}
