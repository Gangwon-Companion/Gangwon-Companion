package com.gangwon.companion.domain.community.repository;

import com.gangwon.companion.domain.community.entity.CommunityPostSave;
import com.gangwon.companion.domain.community.entity.CommunityPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityPostSaveRepository extends JpaRepository<CommunityPostSave, Long> {
    boolean existsByPostIdAndUserId(Long postId, Long userId);
    void deleteByPostIdAndUserId(Long postId, Long userId);
    void deleteByPostId(Long postId);
    long countByPostId(Long postId);

    @Query("select p from CommunityPost p join CommunityPostSave s on s.post = p where s.user.username = :username")
    Page<CommunityPost> findSavedPostsByUsername(@Param("username") String username, Pageable pageable);
}
