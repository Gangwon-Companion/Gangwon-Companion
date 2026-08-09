package com.gangwon.companion.domain.destination.entity;

import com.gangwon.companion.domain.theme.entity.Theme;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access= AccessLevel.PROTECTED)
@Getter
@Table(name = "destinations")
public class Destination {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Long primaryContentId;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private SourceType primarySourceType;

    @Column
    private Integer contentTypeId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 500)
    private String addr1;

    @Column(length = 500)
    private String addr2;

    @Column(precision = 12, scale=8)
    private BigDecimal mapX;

    @Column(precision = 12, scale=8)
    private BigDecimal mapY;

    @Column(length = 1000)
    private String firstImage;

    @Column(length = 1000)
    private String firstImage2;

    @Column(length = 100)
    private String tel;

    @Column(length = 20)
    private String sigunguCode;

    @Column(length = 20)
    private String lclsSystem1;

    @Column(length = 20)
    private String lclsSystem2;

    @Column(length = 20)
    private String lclsSystem3;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theme_id")
    private Theme theme;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Destination(Long primaryContentId,
                       SourceType primarySourceType,
                       Integer contentTypeId,
                       String title,
                       String addr1,
                       String addr2,
                       BigDecimal mapX,
                       BigDecimal mapY,
                       String firstImage,
                       String firstImage2,
                       String tel,
                       String sigunguCode,
                       String lclsSystem1,
                       String lclsSystem2,
                       String lclsSystem3,
                       Theme theme) {
        this.primaryContentId = primaryContentId;
        this.primarySourceType = primarySourceType;
        this.contentTypeId = contentTypeId;
        this.title = title;
        this.addr1 = addr1;
        this.addr2 = addr2;
        this.mapX = mapX;
        this.mapY = mapY;
        this.firstImage = firstImage;
        this.firstImage2 = firstImage2;
        this.tel = tel;
        this.sigunguCode = sigunguCode;
        this.lclsSystem1 = lclsSystem1;
        this.lclsSystem2 = lclsSystem2;
        this.lclsSystem3 = lclsSystem3;
        this.theme = theme;
    }

}
