package com.gangwon.companion.domain.course.service;

import com.gangwon.companion.domain.course.dto.CourseResponse;
import com.gangwon.companion.domain.course.dto.CourseSaveRequest;
import com.gangwon.companion.domain.course.entity.SavedCourse;
import com.gangwon.companion.domain.course.repository.SavedCourseRepository;
import com.gangwon.companion.domain.user.entity.User;
import com.gangwon.companion.domain.user.repository.UserRepository;
import com.gangwon.companion.global.exception.BusinessException;
import com.gangwon.companion.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseService {
    private final SavedCourseRepository savedCourseRepository;
    private final UserRepository userRepository;

    @Transactional
    public CourseResponse save(String username, CourseSaveRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        SavedCourse course = SavedCourse.builder()
                .user(user)
                .name(request.name().trim())
                .build();
        for (int index = 0; index < request.places().size(); index++) {
            CourseSaveRequest.PlaceRequest place = request.places().get(index);
            course.addPlace(place.placeType(), place.placeId(), index + 1,
                    place.day() == null ? 1 : place.day(), place.name(), place.visitTime(), place.address());
        }
        return CourseResponse.from(savedCourseRepository.save(course));
    }
}
