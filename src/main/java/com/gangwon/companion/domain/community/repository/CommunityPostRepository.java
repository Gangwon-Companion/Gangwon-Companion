package com.gangwon.companion.domain.community.repository;

import com.gangwon.companion.domain.community.entity.CommunityPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {
    @Query("select distinct p from CommunityPost p left join p.hashtags h where (:keyword is null or lower(p.title) like lower(concat('%', cast(:keyword as string), '%')) or lower(p.content) like lower(concat('%', cast(:keyword as string), '%')) or lower(h) like lower(concat('%', cast(:keyword as string), '%')))")
    Page<CommunityPost> search(@Param("keyword") String keyword, Pageable pageable);
}
