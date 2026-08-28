package com.gangwon.companion.domain.restaurant.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "restaurant_photos",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_restaurant_photo_restaurant_serial",
                columnNames = {"restaurant_id", "serial_num"}
        ))
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

    @Column(columnDefinition = "text")
    private String originImgUrl;

    @Column(columnDefinition = "text")
    private String smallImgUrl;

    @Column(name = "serial_num", length = 100)
    private String serialNum;

    @Builder
    public RestaurantPhoto(Restaurant restaurant, String url, String originImgUrl, String smallImgUrl, String serialNum) {
        this.restaurant = restaurant;
        this.url = url == null ? originImgUrl : url;
        this.originImgUrl = originImgUrl;
        this.smallImgUrl = smallImgUrl;
        this.serialNum = serialNum;
    }
}
