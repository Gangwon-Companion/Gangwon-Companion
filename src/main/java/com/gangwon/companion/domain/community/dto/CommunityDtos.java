package com.gangwon.companion.domain.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public final class CommunityDtos {
    private CommunityDtos() {}
    public record CreatePostRequest(@NotBlank @Size(max = 120) String title, @NotBlank @Size(max = 10000) String content, Long courseId, @Size(max = 10) Set<@Size(max = 50) String> hashtags, @Size(max = 5) List<ImageRequest> images) {}
    public record UpdatePostRequest(@NotBlank @Size(max = 120) String title, @NotBlank @Size(max = 10000) String content, Long courseId, @Size(max = 10) Set<@Size(max = 50) String> hashtags, @Size(max = 5) List<ImageRequest> images) {}
    public record ImageRequest(@NotBlank @Size(max = 500) String s3Key, @NotBlank @Size(max = 2000) String url, @Size(min = 0, max = 4) int sortOrder) {}
    public record CreateCommentRequest(@NotBlank @Size(max = 1000) String content) {}
    public record PostSummary(Long id, Long postId, String title, String content, String author, String nickname,
                              String authorProfileImageUrl, String profileImageUrl, boolean isMine, boolean liked,
                              boolean saved, int viewCount, int likeCount, long commentCount, long saveCount,
                              int imageCount, List<String> mediaUrls, Long courseId, Set<String> hashtags,
                              LocalDateTime createdAt) {}
    public record CommentResponse(Long id, Long commentId, Long postId, String author, String nickname,
                                  String authorProfileImageUrl, String profileImageUrl, String content, int likeCount,
                                  boolean liked, boolean isMine, LocalDateTime createdAt) {}
    public record MyCommentSummary(Long commentId, Long postId, String postTitle, String postContent, String content,
                                   int likeCount, LocalDateTime createdAt) {}
    public record LikedCommentSummary(Long commentId, Long postId, String postTitle, String postContent, String author,
                                      String nickname, String authorProfileImageUrl, String profileImageUrl,
                                      String content, int likeCount, boolean liked, boolean isMine,
                                      LocalDateTime createdAt) {}
    public record PostDetail(Long id, Long postId, String title, String content, String author, String nickname,
                             String authorProfileImageUrl, String profileImageUrl, boolean isMine, boolean liked,
                             boolean saved, int viewCount, int likeCount, long commentCount, long saveCount,
                             Long courseId, Set<String> hashtags, LocalDateTime createdAt, LocalDateTime updatedAt,
                             List<String> mediaUrls, List<ImageResponse> images, List<CommentResponse> comments) {}
    public record ImageResponse(String s3Key, String url, int sortOrder) {}
    public record PresignedUploadResponse(String s3Key, String uploadUrl, int expiresInSeconds) {}
    public record PresignedUploadRequest(@NotBlank @Size(max = 255) String originalFileName, @NotBlank @Pattern(regexp = "image/(jpeg|png|webp)") String contentType) {}
}
