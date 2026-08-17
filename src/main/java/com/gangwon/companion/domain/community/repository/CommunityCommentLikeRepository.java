package com.gangwon.companion.domain.community.repository;

import com.gangwon.companion.domain.community.entity.CommunityCommentLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityCommentLikeRepository extends JpaRepository<CommunityCommentLike, Long> {
    boolean existsByCommentIdAndUserId(Long commentId, Long userId);
    void deleteByCommentIdAndUserId(Long commentId, Long userId);
}
