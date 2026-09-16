package com.gangwon.companion.domain.travelprofile.entity;

import com.gangwon.companion.domain.travelprofile.dto.AiTravelProfileResponse;
import com.gangwon.companion.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "travel_profiles", uniqueConstraints = @UniqueConstraint(name = "uk_travel_profile_user", columnNames = "user_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TravelProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ProfileStatus status;
    @Enumerated(EnumType.STRING) @Column(length = 50)
    private TravelerType travelerType;
    @Column(length = 100) private String title;
    @Column(length = 500) private String description;
    @ElementCollection(fetch = FetchType.EAGER) @CollectionTable(name = "travel_profile_tags", joinColumns = @JoinColumn(name = "profile_id"))
    @OrderColumn(name = "tag_order") @Column(name = "tag", nullable = false, length = 30)
    private List<String> tags = new ArrayList<>();
    @ElementCollection(fetch = FetchType.EAGER) @CollectionTable(name = "travel_profile_evidences", joinColumns = @JoinColumn(name = "profile_id"))
    @OrderColumn(name = "evidence_order") @Column(name = "evidence", nullable = false, length = 200)
    private List<String> evidences = new ArrayList<>();
    private Double confidence;
    @Column(length = 50) private String analysisVersion;
    private Instant analyzedAt;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;

    private TravelProfile(User user) {
        this.user = user;
        this.status = ProfileStatus.NOT_ANALYZED;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public static TravelProfile forUser(User user) { return new TravelProfile(user); }

    public void apply(AiTravelProfileResponse response) {
        status = ProfileStatus.valueOf(response.status().name());
        travelerType = response.travelerType() == null ? null : TravelerType.valueOf(response.travelerType().name());
        title = response.title();
        description = response.description();
        tags.clear(); tags.addAll(response.tags());
        evidences.clear(); evidences.addAll(response.evidences());
        confidence = response.confidence();
        analysisVersion = response.analysisVersion();
        analyzedAt = Instant.now();
        updatedAt = analyzedAt;
    }

    public enum ProfileStatus { NOT_ANALYZED, COMPLETED, INSUFFICIENT_DATA }
    public enum TravelerType { NATURE_HEALING, PET_COMPANION, LOCAL_FOOD_EXPLORER, ACTIVITY_ADVENTURE, CULTURE_EXPLORER, BALANCED_TRAVELER }
}
