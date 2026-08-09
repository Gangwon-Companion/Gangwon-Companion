package com.gangwon.companion.domain.course.repository;

import com.gangwon.companion.domain.course.entity.SavedCourse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedCourseRepository extends JpaRepository<SavedCourse, Long> {

    long countByUserUsername(String username);
}
