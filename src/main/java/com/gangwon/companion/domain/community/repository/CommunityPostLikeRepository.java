package com.gangwon.companion.domain.community.repository;

import com.gangwon.companion.domain.community.entity.CommunityPostLike;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CommunityPostLikeRepository extends JpaRepository<CommunityPostLike, Long> {
    boolean existsByPostIdAndUserId(Long postId, Long userId);
    void deleteByPostIdAndUserId(Long postId, Long userId);
}
