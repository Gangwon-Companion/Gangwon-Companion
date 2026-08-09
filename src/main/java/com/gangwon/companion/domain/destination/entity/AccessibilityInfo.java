package com.gangwon.companion.domain.destination.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "accessibility_infos",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_accessibility_info_destination_content",
                        columnNames = {"destination_id", "content_id"}
                )
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class AccessibilityInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id", nullable = false)
    private Destination destination;

    @Column(nullable = false)
    private Long contentId;

    @Column(columnDefinition = "text")
    private String parking;

    @Column(columnDefinition = "text")
    private String route;

    @Column(columnDefinition = "text")
    private String entrance;

    @Column(columnDefinition = "text")
    private String elevator;

    @Column(columnDefinition = "text")
    private String restroom;

    @Column(columnDefinition = "text")
    private String wheelchair;

    @Column(columnDefinition = "text")
    private String braileBlock;

    @Column(columnDefinition = "text")
    private String helpDog;

    @Column(columnDefinition = "text")
    private String guideHuman;

    @Column(columnDefinition = "text")
    private String rawAccessibilityJson;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public AccessibilityInfo(
            Destination destination,
            Long contentId,
            String parking,
            String route,
            String entrance,
            String elevator,
            String restroom,
            String wheelchair,
            String braileBlock,
            String helpDog,
            String guideHuman,
            String rawAccessibilityJson
    ) {
        this.destination = destination;
        this.contentId = contentId;
        this.parking = parking;
        this.route = route;
        this.entrance = entrance;
        this.elevator = elevator;
        this.restroom = restroom;
        this.wheelchair = wheelchair;
        this.braileBlock = braileBlock;
        this.helpDog = helpDog;
        this.guideHuman = guideHuman;
        this.rawAccessibilityJson = rawAccessibilityJson;
    }
}
