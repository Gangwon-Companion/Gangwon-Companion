package com.gangwon.companion.domain.search.entity;

import com.gangwon.companion.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "search_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String keyword;

    @Column(length = 30)
    private String region;

    @Column(nullable = false)
    private LocalDateTime searchedAt;

    @Builder
    public SearchHistory(User user, String keyword, String region, LocalDateTime searchedAt) {
        this.user = user;
        this.keyword = keyword;
        this.region = region;
        this.searchedAt = searchedAt;
    }

    @PrePersist
    void prePersist() {
        if (searchedAt == null) {
            searchedAt = LocalDateTime.now();
        }
    }
}
