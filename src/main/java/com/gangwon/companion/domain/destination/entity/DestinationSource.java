package com.gangwon.companion.domain.destination.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "destination_sources")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@EntityListeners(AuditingEntityListener.class)
public class DestinationSource {
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
    private  String rawListJson;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public DestinationSource(
                             String rawListJson,
                             Destination destination,
                             SourceType sourceType,
                             Long contentId,
                             Integer contentTypeId) {
        this.rawListJson = rawListJson;
        this.destination = destination;
        this.sourceType = sourceType;
        this.contentId = contentId;
        this.contentTypeId = contentTypeId;
    }
}
