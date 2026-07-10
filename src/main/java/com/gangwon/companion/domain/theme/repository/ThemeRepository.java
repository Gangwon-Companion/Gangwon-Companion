package com.gangwon.companion.domain.theme.repository;

import com.gangwon.companion.domain.theme.entity.Theme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ThemeRepository extends JpaRepository<Theme, Long> {
    List<Theme> findAllByOrderByDisplayOrderAsc();

    Optional<Theme> findByCode(String code);
}
