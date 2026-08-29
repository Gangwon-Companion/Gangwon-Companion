package com.gangwon.companion.domain.course.entity;

import com.gangwon.companion.domain.travel.entity.PlaceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "course_places", uniqueConstraints = {
        @UniqueConstraint(name = "uk_course_place_order", columnNames = {"course_id", "visit_order"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoursePlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private SavedCourse course;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlaceType placeType;

    @Column(nullable = false)
    private Long placeId;

    @Column(name = "visit_order", nullable = false)
    private Integer visitOrder;

    @Column(name = "travel_day")
    private Integer day;

    @Column(name = "place_name", length = 100)
    private String name;

    @Column(name = "visit_time", length = 30)
    private String visitTime;

    @Column(length = 255)
    private String address;

    @Builder
    public CoursePlace(SavedCourse course, PlaceType placeType, Long placeId, Integer visitOrder,
                       Integer day, String name, String visitTime, String address) {
        this.course = course;
        this.placeType = placeType;
        this.placeId = placeId;
        this.visitOrder = visitOrder;
        this.day = day;
        this.name = name;
        this.visitTime = visitTime;
        this.address = address;
    }
}
