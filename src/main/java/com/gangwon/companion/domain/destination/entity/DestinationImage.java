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
@Table(name = "destination_images",
        uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_destination_image_source_content_serial",
                columnNames = {"source_type", "content_id", "serial_num"}
        )
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class DestinationImage {
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

    @Column(columnDefinition = "text")
    private String originImgUrl;

    @Column(columnDefinition = "text")
    private String smallImgUrl;

    @Column(length = 100)
    private String serialNum;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public DestinationImage(Destination destination,
                            SourceType sourceType,
                            Long contentId,
                            String originImgUrl,
                            String smallImgUrl,
                            String serialNum) {
        this.destination = destination;
        this.sourceType = sourceType;
        this.contentId = contentId;
        this.originImgUrl = originImgUrl;
        this.smallImgUrl = smallImgUrl;
        this.serialNum = serialNum;
    }
}
