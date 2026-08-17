package com.gangwon.companion.domain.community.repository;

import com.gangwon.companion.domain.community.entity.CommunityPostSave;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityPostSaveRepository extends JpaRepository<CommunityPostSave, Long> {
    boolean existsByPostIdAndUserId(Long postId, Long userId);
    void deleteByPostIdAndUserId(Long postId, Long userId);
    long countByPostId(Long postId);
}
