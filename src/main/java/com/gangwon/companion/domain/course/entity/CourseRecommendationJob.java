package com.gangwon.companion.domain.course.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "course_recommendation_jobs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseRecommendationJob {
    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Lob
    private String resultJson;

    @Column(length = 50)
    private String errorCode;

    @Column(length = 500)
    private String errorMessage;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private CourseRecommendationJob(UUID id, String username) {
        this.id = id;
        this.username = username;
        this.status = Status.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public static CourseRecommendationJob pending(String username) {
        return new CourseRecommendationJob(UUID.randomUUID(), username);
    }

    public void markRunning() {
        status = Status.RUNNING;
        touch();
    }

    public void complete(String resultJson) {
        status = Status.COMPLETED;
        this.resultJson = resultJson;
        errorCode = null;
        errorMessage = null;
        touch();
    }

    public void fail(String errorCode, String errorMessage) {
        status = Status.FAILED;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        touch();
    }

    private void touch() {
        updatedAt = Instant.now();
    }

    public enum Status {
        PENDING, RUNNING, COMPLETED, FAILED
    }
}
