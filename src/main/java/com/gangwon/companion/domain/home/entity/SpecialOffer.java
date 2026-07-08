package com.gangwon.companion.domain.home.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "special_offers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpecialOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 30)
    private String region;

    @Column(nullable = false, length = 30)
    private String category;

    @Column(nullable = false)
    private int originalPrice;

    @Column(nullable = false)
    private int salePrice;

    @Column(nullable = false)
    private int discountRate;

    @Column(length = 255)
    private String reason;

    @Column(length = 500)
    private String imageUrl;

    @Column(length = 500)
    private String linkUrl;

    @Builder
    public SpecialOffer(
            String title,
            String region,
            String category,
            int originalPrice,
            int salePrice,
            int discountRate,
            String reason,
            String imageUrl,
            String linkUrl
    ) {
        this.title = title;
        this.region = region;
        this.category = category;
        this.originalPrice = originalPrice;
        this.salePrice = salePrice;
        this.discountRate = discountRate;
        this.reason = reason;
        this.imageUrl = imageUrl;
        this.linkUrl = linkUrl;
    }
}
