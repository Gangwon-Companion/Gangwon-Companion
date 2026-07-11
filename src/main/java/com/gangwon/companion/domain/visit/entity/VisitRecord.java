package com.gangwon.companion.domain.visit.entity;

import com.gangwon.companion.domain.travel.entity.PlaceType;
import com.gangwon.companion.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "visit_records", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_visited_place", columnNames = {"user_id", "place_type", "place_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class VisitRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlaceType placeType;

    @Column(nullable = false)
    private Long placeId;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime visitedAt;

    @Builder
    public VisitRecord(User user, PlaceType placeType, Long placeId) {
        this.user = user;
        this.placeType = placeType;
        this.placeId = placeId;
    }
}
