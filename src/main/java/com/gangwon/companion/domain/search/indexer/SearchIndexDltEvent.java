package com.gangwon.companion.domain.search.indexer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "search_index_dlt_events",
        uniqueConstraints = @UniqueConstraint(name = "uk_search_index_dlt_place", columnNames = {"domain", "place_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchIndexDltEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String domain;

    @Column(name = "place_id", nullable = false)
    private Long placeId;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private SearchIndexDltEvent(String domain, long placeId, String error) {
        this.domain = domain;
        this.placeId = placeId;
        this.attempts = 0;
        this.lastError = error;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public static SearchIndexDltEvent pending(String domain, long placeId, String error) {
        return new SearchIndexDltEvent(domain, placeId, error);
    }

    public void recordFailure(String error) {
        attempts++;
        lastError = error;
        updatedAt = LocalDateTime.now();
    }
}
