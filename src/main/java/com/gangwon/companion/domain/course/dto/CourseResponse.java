package com.gangwon.companion.domain.course.dto;

import com.gangwon.companion.domain.course.entity.SavedCourse;
import java.util.List;

public record CourseResponse(Long id, String name, List<CoursePlaceResponse> places) {
    public static CourseResponse from(SavedCourse course) {
        return new CourseResponse(course.getId(), course.getName(), course.getPlaces().stream()
                .map(p -> new CoursePlaceResponse(p.getId(), p.getPlaceType().name(), p.getPlaceId(),
                        p.getVisitOrder(), p.getDay() == null ? 1 : p.getDay(), p.getName(),
                        p.getVisitTime(), p.getAddress())).toList());
    }
    public record CoursePlaceResponse(Long id, String placeType, Long placeId, Integer visitOrder,
                                      Integer day, String name, String visitTime, String address) {}
}
