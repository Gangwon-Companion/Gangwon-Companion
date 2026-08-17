package com.gangwon.companion.domain.community.entity;

import com.gangwon.companion.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Entity @Table(name = "community_comments") @Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class CommunityComment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "post_id", nullable = false) private CommunityPost post;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(nullable = false, columnDefinition = "TEXT") private String content;
    @Column(nullable = false) private int likeCount;
    @CreatedDate @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @Builder public CommunityComment(CommunityPost post, User user, String content) { this.post = post; this.user = user; this.content = content; }
    public void update(String content) { this.content = content; }
    public void increaseLikeCount() { likeCount++; }
    public void decreaseLikeCount() { if (likeCount > 0) likeCount--; }
}
