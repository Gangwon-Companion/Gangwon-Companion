package com.gangwon.companion.domain.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public final class CommunityDtos {
    private CommunityDtos() {}
    public record CreatePostRequest(@NotBlank @Size(max = 120) String title, @NotBlank String content, Long courseId, Set<@Size(max = 50) String> hashtags, List<ImageRequest> images) {}
    public record UpdatePostRequest(@NotBlank @Size(max = 120) String title, @NotBlank String content, Long courseId, Set<@Size(max = 50) String> hashtags, List<ImageRequest> images) {}
    public record ImageRequest(@NotBlank @Size(max = 500) String s3Key, @NotBlank String url, int sortOrder) {}
    public record CreateCommentRequest(@NotBlank String content) {}
    public record PostSummary(Long id, String title, String author, boolean isMine, boolean liked, int viewCount, int likeCount, int imageCount, Long courseId, Set<String> hashtags, LocalDateTime createdAt) {}
    public record CommentResponse(Long id, String author, String content, LocalDateTime createdAt) {}
    public record PostDetail(Long id, String title, String content, String author, boolean isMine, boolean liked, int viewCount, int likeCount, Long courseId, Set<String> hashtags, LocalDateTime createdAt, LocalDateTime updatedAt, List<ImageResponse> images, List<CommentResponse> comments) {}
    public record ImageResponse(String s3Key, String url, int sortOrder) {}
    public record PresignedUploadResponse(String s3Key, String uploadUrl, int expiresInSeconds) {}
    public record PresignedUploadRequest(@NotBlank String originalFileName, @NotBlank String contentType) {}
}
