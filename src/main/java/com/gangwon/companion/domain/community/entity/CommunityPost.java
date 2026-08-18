package com.gangwon.companion.domain.community.entity;

import com.gangwon.companion.domain.course.entity.SavedCourse;
import com.gangwon.companion.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "community_posts", indexes = {
        @Index(name = "idx_community_post_created_at", columnList = "created_at"),
        @Index(name = "idx_community_post_course_id", columnList = "course_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class CommunityPost {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private SavedCourse course;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private int viewCount;

    @Column(nullable = false)
    private int likeCount;

    @ElementCollection
    @CollectionTable(name = "community_post_hashtags", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "hashtag", nullable = false, length = 50)
    private Set<String> hashtags = new LinkedHashSet<>();

    @CreatedDate @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @LastModifiedDate @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private List<CommunityPostImage> images = new ArrayList<>();

    @Builder
    public CommunityPost(User user, SavedCourse course, String title, String content, Set<String> hashtags) {
        this.user = user; this.course = course; this.title = title; this.content = content;
        if (hashtags != null) this.hashtags.addAll(hashtags);
    }

    public void update(String title, String content, SavedCourse course, Set<String> hashtags) {
        this.title = title; this.content = content; this.course = course;
        this.hashtags.clear(); if (hashtags != null) this.hashtags.addAll(hashtags);
    }
    public void increaseViewCount() { viewCount++; }
    public void increaseLikeCount() { likeCount++; }
    public void decreaseLikeCount() { if (likeCount > 0) likeCount--; }
    public void addImage(CommunityPostImage image) { images.add(image); }
    public void clearImages() { images.clear(); }
}
