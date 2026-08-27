package com.gangwon.companion.domain.community.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "community_post_images", uniqueConstraints = @UniqueConstraint(
        name = "uk_community_post_image_key", columnNames = "s3_key"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityPostImage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private CommunityPost post;
    @Column(name = "s3_key", nullable = false, length = 500)
    private String s3Key;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;
    @Column(nullable = false)
    private int sortOrder;

    @Builder
    public CommunityPostImage(CommunityPost post, String s3Key, String url, int sortOrder) {
        this.post = post; this.s3Key = s3Key; this.url = url; this.sortOrder = sortOrder;
    }

    public void update(String url, int sortOrder) {
        this.url = url; this.sortOrder = sortOrder;
    }
}
