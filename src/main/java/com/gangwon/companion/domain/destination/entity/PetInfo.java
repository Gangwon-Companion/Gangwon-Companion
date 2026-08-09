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
@Table(name = "pet_infos",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_pet_info_destination_content",
                        columnNames = {"destination_id", "content_id"}
                )
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class PetInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id", nullable = false)
    private Destination destination;

    @Column(nullable = false)
    private Long contentId;

    @Column(length = 255)
    private String accompanyType;

    @Column(columnDefinition = "text")
    private String needItems;

    @Column(columnDefinition = "text")
    private String petFacilities;

    @Column(columnDefinition = "text")
    private String caution;

    @Column(columnDefinition = "text")
    private String accidentRisk;

    @Column(columnDefinition = "text")
    private String rawPetJson;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public PetInfo(
            Destination destination,
            Long contentId,
            String accompanyType,
            String needItems,
            String petFacilities,
            String caution,
            String accidentRisk,
            String rawPetJson
    ) {
        this.destination = destination;
        this.contentId = contentId;
        this.accompanyType = accompanyType;
        this.needItems = needItems;
        this.petFacilities = petFacilities;
        this.caution = caution;
        this.accidentRisk = accidentRisk;
        this.rawPetJson = rawPetJson;
    }
}
