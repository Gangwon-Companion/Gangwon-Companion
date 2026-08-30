package com.gangwon.companion.domain.community.repository;

import com.gangwon.companion.domain.community.entity.CommunityComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommunityCommentRepository extends JpaRepository<CommunityComment, Long> {
    List<CommunityComment> findAllByPostIdOrderByCreatedAtAsc(Long postId);
    Page<CommunityComment> findAllByUserUsername(String username, Pageable pageable);
    long countByPostId(Long postId);
    void deleteByPostId(Long postId);
}
