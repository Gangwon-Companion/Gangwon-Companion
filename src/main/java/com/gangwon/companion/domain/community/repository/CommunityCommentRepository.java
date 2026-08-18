package com.gangwon.companion.domain.community.repository;

import com.gangwon.companion.domain.community.entity.CommunityComment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommunityCommentRepository extends JpaRepository<CommunityComment, Long> {
    List<CommunityComment> findAllByPostIdOrderByCreatedAtAsc(Long postId);
}
