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
@Table(name = "destination_details",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_destination_detail_source_content",
                        columnNames = {"source_type", "content_id"}
                )
        }
        )
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class DestinationDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id", nullable = false)
    private Destination destination;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SourceType sourceType;

    @Column(nullable = false)
    private Long contentId;

    @Column
    private Integer contentTypeId;

    @Column(columnDefinition = "text")
    private String overview;

    @Column(columnDefinition = "text")
    private String homepage;

    @Column(length = 500)
    private String usageTime;

    @Column(length = 500)
    private String restDate;

    @Column(length = 500)
    private String parking;

    @Column(length = 500)
    private String inquiry;

    @Column(columnDefinition = "text")
    private String rawCommonJson;

    @Column(columnDefinition = "text")
    private String rawIntroJson;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public DestinationDetail(
            Destination destination,
            SourceType sourceType,
            Long contentId,
            Integer contentTypeId,
            String overview,
            String homepage,
            String usageTime,
            String restDate,
            String parking,
            String inquiry,
            String rawCommonJson,
            String rawIntroJson
    ) {
        this.destination = destination;
        this.sourceType = sourceType;
        this.contentId = contentId;
        this.contentTypeId = contentTypeId;
        this.overview = overview;
        this.homepage = homepage;
        this.usageTime = usageTime;
        this.restDate = restDate;
        this.parking = parking;
        this.inquiry = inquiry;
        this.rawCommonJson = rawCommonJson;
        this.rawIntroJson = rawIntroJson;
    }

}
