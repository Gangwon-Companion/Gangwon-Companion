package com.gangwon.companion.domain.touristcongestion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "tourist_congestion_rates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class TouristCongestionRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "area_code", nullable = false, length = 10)
    private String areaCode;

    @Column(name = "signgu_code", nullable = false, length = 10)
    private String signguCode;

    @Column(name = "area_name")
    private String areaName;

    @Column(name = "signgu_name")
    private String signguName;

    @Column(name = "attraction_name")
    private String attractionName;

    @Column(name = "congestion_rate")
    private Double congestionRate;

    @Column(name = "base_date", length = 20)
    private String baseDate;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "external_key", nullable = false, unique = true)
    private String externalKey;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Builder
    public TouristCongestionRate(String areaCode, String signguCode, String areaName, String signguName,
                                 String attractionName, Double congestionRate, String baseDate, String rawPayload,
                                 String externalKey) {
        this.areaCode = areaCode;
        this.signguCode = signguCode;
        this.areaName = areaName;
        this.signguName = signguName;
        this.attractionName = attractionName;
        this.congestionRate = congestionRate;
        this.baseDate = baseDate;
        this.rawPayload = rawPayload;
        this.externalKey = externalKey;
    }

    public void updateFromApi(String areaCode, String signguCode, String areaName, String signguName,
                              String attractionName, Double congestionRate, String baseDate, String rawPayload) {
        this.areaCode = areaCode;
        this.signguCode = signguCode;
        this.areaName = areaName;
        this.signguName = signguName;
        this.attractionName = attractionName;
        this.congestionRate = congestionRate;
        this.baseDate = baseDate;
        this.rawPayload = rawPayload;
    }
}
