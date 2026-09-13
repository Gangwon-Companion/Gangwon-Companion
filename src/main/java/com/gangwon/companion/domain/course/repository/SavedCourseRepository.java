package com.gangwon.companion.domain.course.repository;

import com.gangwon.companion.domain.course.entity.SavedCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

public interface SavedCourseRepository extends JpaRepository<SavedCourse, Long> {

    long countByUserUsername(String username);
    List<SavedCourse> findAllByUserUsernameOrderByCreatedAtDesc(String username);
    @EntityGraph(attributePaths = "places")
    List<SavedCourse> findByUserUsernameOrderByCreatedAtDesc(String username, Pageable pageable);
}
