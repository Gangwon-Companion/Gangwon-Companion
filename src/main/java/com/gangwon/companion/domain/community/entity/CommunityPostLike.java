package com.gangwon.companion.domain.community.entity;

import com.gangwon.companion.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "community_post_likes", uniqueConstraints = @UniqueConstraint(name = "uk_post_like_user", columnNames = {"post_id", "user_id"}))
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityPostLike {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "post_id", nullable = false) private CommunityPost post;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Builder public CommunityPostLike(CommunityPost post, User user) { this.post = post; this.user = user; }
}
