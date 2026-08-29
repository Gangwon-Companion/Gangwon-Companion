package com.gangwon.companion.domain.course.entity;

import com.gangwon.companion.domain.user.entity.User;
import com.gangwon.companion.domain.travel.entity.PlaceType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "saved_courses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class SavedCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String name;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CoursePlace> places = new ArrayList<>();

    @Builder
    public SavedCourse(User user, String name) {
        this.user = user;
        this.name = name;
    }

    public void addPlace(PlaceType placeType, Long placeId, Integer visitOrder,
                         Integer day, String name, String visitTime, String address) {
        places.add(CoursePlace.builder()
                .course(this)
                .placeType(placeType)
                .placeId(placeId)
                .visitOrder(visitOrder)
                .day(day)
                .name(name)
                .visitTime(visitTime)
                .address(address)
                .build());
    }
}
