package com.gangwon.companion.domain.lodging.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lodgings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Lodging {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 50)
    private String region;

    @Column(nullable = false)
    private Long price;

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

    @OneToMany(mappedBy = "lodging", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LodgingPhoto> photos = new ArrayList<>();

    @OneToMany(mappedBy = "lodging", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LodgingReview> reviews = new ArrayList<>();

    @Builder
    public Lodging(String name, String description, String region, Long price, Double rating,
                   String thumbnailUrl, String address, Double latitude, Double longitude) {
        this.name = name;
        this.description = description;
        this.region = region;
        this.price = price;
        this.rating = rating;
        this.thumbnailUrl = thumbnailUrl;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
