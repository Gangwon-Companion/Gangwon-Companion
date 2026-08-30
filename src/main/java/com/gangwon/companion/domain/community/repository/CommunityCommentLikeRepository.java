package com.gangwon.companion.domain.community.repository;

import com.gangwon.companion.domain.community.entity.CommunityCommentLike;
import com.gangwon.companion.domain.community.entity.CommunityComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityCommentLikeRepository extends JpaRepository<CommunityCommentLike, Long> {
    boolean existsByCommentIdAndUserId(Long commentId, Long userId);
    void deleteByCommentIdAndUserId(Long commentId, Long userId);
    void deleteByCommentPostId(Long postId);

    @Query("select c from CommunityComment c join CommunityCommentLike l on l.comment = c where l.user.username = :username")
    Page<CommunityComment> findLikedCommentsByUsername(@Param("username") String username, Pageable pageable);
}
