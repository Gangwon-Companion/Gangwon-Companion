package com.gangwon.companion.domain.user.dto.response;

import com.gangwon.companion.domain.community.dto.CommunityDtos.PostSummary;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record MyCommunityPostResponse(
        Long id,
        Long postId,
        String title,
        String content,
        String author,
        String nickname,
        String authorProfileImageUrl,
        String profileImageUrl,
        boolean isMine,
        boolean liked,
        boolean saved,
        int viewCount,
        int likeCount,
        long commentCount,
        long saveCount,
        int imageCount,
        List<String> mediaUrls,
        Long courseId,
        Set<String> hashtags,
        LocalDateTime createdAt
) {
    public static MyCommunityPostResponse from(PostSummary summary) {
        return new MyCommunityPostResponse(
                summary.id(),
                summary.postId(),
                summary.title(),
                summary.content(),
                summary.author(),
                summary.nickname(),
                summary.authorProfileImageUrl(),
                summary.profileImageUrl(),
                summary.isMine(),
                summary.liked(),
                summary.saved(),
                summary.viewCount(),
                summary.likeCount(),
                summary.commentCount(),
                summary.saveCount(),
                summary.imageCount(),
                summary.mediaUrls(),
                summary.courseId(),
                summary.hashtags(),
                summary.createdAt()
        );
    }
}
