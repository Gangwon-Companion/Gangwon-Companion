package com.gangwon.companion.domain.activity.repository;

import com.gangwon.companion.domain.activity.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;

public interface ActivityRepository extends JpaRepository<Activity, Long>, JpaSpecificationExecutor<Activity> {

    List<Activity> findAllByTourContentIdIn(Collection<Long> tourContentIds);
}
