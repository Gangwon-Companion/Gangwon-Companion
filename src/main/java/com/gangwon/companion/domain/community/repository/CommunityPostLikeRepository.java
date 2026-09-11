package com.gangwon.companion.domain.community.repository;

import com.gangwon.companion.domain.community.entity.CommunityPostLike;
import com.gangwon.companion.domain.community.entity.CommunityPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityPostLikeRepository extends JpaRepository<CommunityPostLike, Long> {
    boolean existsByPostIdAndUserId(Long postId, Long userId);
    void deleteByPostIdAndUserId(Long postId, Long userId);
    void deleteByPostId(Long postId);

    @Query("select p from CommunityPost p join CommunityPostLike l on l.post = p where l.user.username = :username")
    Page<CommunityPost> findLikedPostsByUsername(@Param("username") String username, Pageable pageable);
}
