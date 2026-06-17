package com.gangwon.companion.domain.lodging.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "lodging_reviews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class LodgingReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lodging_id", nullable = false)
    private Lodging lodging;

    @Column(nullable = false, length = 20)
    private String nickname;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Double rating;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public LodgingReview(Lodging lodging, String nickname, String content, Double rating) {
        this.lodging = lodging;
        this.nickname = nickname;
        this.content = content;
        this.rating = rating;
    }
}
