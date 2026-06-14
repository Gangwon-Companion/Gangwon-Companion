package com.gangwon.companion.domain.restaurant.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "restaurants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 50)
    private String menuType;

    @Column(nullable = false, length = 50)
    private String region;

    @Column(nullable = false)
    private Double rating;

    private String thumbnailUrl;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RestaurantPhoto> photos = new ArrayList<>();

    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RestaurantReview> reviews = new ArrayList<>();

    @Builder
    public Restaurant(String name, String menuType, String region, Double rating,
                      String thumbnailUrl, String address, Double latitude, Double longitude) {
        this.name = name;
        this.menuType = menuType;
        this.region = region;
        this.rating = rating;
        this.thumbnailUrl = thumbnailUrl;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
