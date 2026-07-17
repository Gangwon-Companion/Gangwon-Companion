package com.gangwon.companion.domain.activity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "activities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long tourContentId;

    @Column(nullable = false)
    private int contentTypeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActivityCategory category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 50)
    private String region;

    @Column(length = 300)
    private String address;

    @Column(length = 500)
    private String imageUrl;

    @Column(length = 500)
    private String thumbnailUrl;

    @Column(length = 100)
    private String telephone;

    @Column(length = 1000)
    private String homepageUrl;

    @Column(columnDefinition = "TEXT")
    private String overview;

    @Column(length = 500)
    private String operatingHours;

    @Column(length = 500)
    private String restDate;

    @Column(length = 500)
    private String usageFee;

    @Column(length = 500)
    private String parkingInfo;

    @Column(length = 1000)
    private String reservationUrl;

    private Double latitude;

    private Double longitude;

    private LocalDateTime tourModifiedAt;

    private LocalDateTime detailSyncedAt;

    private LocalDateTime syncedAt;

    public Activity(Long tourContentId, int contentTypeId) {
        this.tourContentId = tourContentId;
        this.contentTypeId = contentTypeId;
    }

    public void updateListData(
            ActivityCategory category,
            String title,
            String region,
            String address,
            String imageUrl,
            String thumbnailUrl,
            String telephone,
            Double latitude,
            Double longitude,
            LocalDateTime tourModifiedAt
    ) {
        this.category = category;
        this.title = title;
        this.region = region;
        this.address = address;
        this.imageUrl = imageUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.telephone = telephone;
        this.latitude = latitude;
        this.longitude = longitude;
        this.tourModifiedAt = tourModifiedAt;
        this.syncedAt = LocalDateTime.now();
    }

    public void updateDetail(
            String homepageUrl,
            String overview,
            String telephone,
            String operatingHours,
            String restDate,
            String usageFee,
            String parkingInfo,
            String reservationUrl
    ) {
        this.homepageUrl = homepageUrl;
        this.overview = overview;
        if (telephone != null && !telephone.isBlank()) {
            this.telephone = telephone;
        }
        this.operatingHours = operatingHours;
        this.restDate = restDate;
        this.usageFee = usageFee;
        this.parkingInfo = parkingInfo;
        this.reservationUrl = reservationUrl;
        this.detailSyncedAt = LocalDateTime.now();
    }
}
