package com.gangwon.companion.domain.lodging.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lodging_photos")
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

    @Builder
    public LodgingPhoto(Lodging lodging, String url) {
        this.lodging = lodging;
        this.url = url;
    }
}
