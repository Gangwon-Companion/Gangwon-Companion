package com.gangwon.companion.domain.lodging.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lodging_photos",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_lodging_photo_lodging_serial",
                columnNames = {"lodging_id", "serial_num"}
        ))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LodgingPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lodging_id", nullable = false)
    private Lodging lodging;

    @Column(nullable = false)
    private String url;

    @Column(columnDefinition = "text")
    private String originImgUrl;

    @Column(columnDefinition = "text")
    private String smallImgUrl;

    @Column(name = "serial_num", length = 100)
    private String serialNum;

    @Builder
    public LodgingPhoto(Lodging lodging, String url, String originImgUrl, String smallImgUrl, String serialNum) {
        this.lodging = lodging;
        this.url = url == null ? originImgUrl : url;
        this.originImgUrl = originImgUrl;
        this.smallImgUrl = smallImgUrl;
        this.serialNum = serialNum;
    }
}
