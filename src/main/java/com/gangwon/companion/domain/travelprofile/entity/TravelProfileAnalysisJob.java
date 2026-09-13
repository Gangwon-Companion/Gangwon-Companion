package com.gangwon.companion.domain.travelprofile.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "travel_profile_analysis_jobs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TravelProfileAnalysisJob {
    @Id private UUID id;
    @Column(nullable = false, length = 100) private String username;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Status status;
    @Column(length = 50) private String errorCode;
    @Column(length = 500) private String errorMessage;
    private Long profileId;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;

    private TravelProfileAnalysisJob(String username) {
        id = UUID.randomUUID(); this.username = username; status = Status.PENDING;
        createdAt = Instant.now(); updatedAt = createdAt;
    }
    public static TravelProfileAnalysisJob pending(String username) { return new TravelProfileAnalysisJob(username); }
    public void markRunning() { status = Status.RUNNING; touch(); }
    public void complete(Long profileId) { status = Status.COMPLETED; this.profileId = profileId; errorCode = null; errorMessage = null; touch(); }
    public void fail(String code, String message) { status = Status.FAILED; errorCode = code; errorMessage = message; touch(); }
    private void touch() { updatedAt = Instant.now(); }
    public enum Status { PENDING, RUNNING, COMPLETED, FAILED }
}
