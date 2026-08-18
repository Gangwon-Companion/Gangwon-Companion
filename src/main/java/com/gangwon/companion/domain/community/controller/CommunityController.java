package com.gangwon.companion.domain.community.controller;

import com.gangwon.companion.domain.community.dto.CommunityDtos.*;
import com.gangwon.companion.domain.community.service.CommunityService;
import com.gangwon.companion.global.storage.S3FileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/community")
@RequiredArgsConstructor
public class CommunityController {
    private final CommunityService communityService;
    private final S3FileService s3FileService;

    @GetMapping("/posts")
    public Page<PostSummary> list(@RequestParam(required = false) String keyword,
                                  @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
                                  Authentication authentication) {
        return communityService.list(authentication == null ? null : authentication.getName(), keyword, pageable);
    }
    @GetMapping("/posts/{postId}")
    public PostDetail get(@PathVariable Long postId, Authentication authentication) { return communityService.get(authentication == null ? null : authentication.getName(), postId); }
    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public PostDetail create(@Valid @RequestBody CreatePostRequest request, Authentication authentication) { return communityService.create(authentication.getName(), request); }
    @PutMapping("/posts/{postId}")
    public PostDetail update(@PathVariable Long postId, @Valid @RequestBody UpdatePostRequest request, Authentication authentication) { return communityService.update(authentication.getName(), postId, request); }
    @DeleteMapping("/posts/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long postId, Authentication authentication) { communityService.delete(authentication.getName(), postId); }
    @PostMapping("/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse comment(@PathVariable Long postId, @Valid @RequestBody CreateCommentRequest request, Authentication authentication) { return communityService.comment(authentication.getName(), postId, request); }
    @PostMapping("/posts/{postId}/likes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void like(@PathVariable Long postId, Authentication authentication) { communityService.like(authentication.getName(), postId); }
    @DeleteMapping("/posts/{postId}/likes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlike(@PathVariable Long postId, Authentication authentication) { communityService.unlike(authentication.getName(), postId); }
    @PostMapping("/posts/{postId}/saves")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void save(@PathVariable Long postId, Authentication authentication) { communityService.save(authentication.getName(), postId); }
    @DeleteMapping("/posts/{postId}/saves")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsave(@PathVariable Long postId, Authentication authentication) { communityService.unsave(authentication.getName(), postId); }
    @PutMapping("/comments/{commentId}")
    public CommentResponse updateComment(@PathVariable Long commentId, @Valid @RequestBody CreateCommentRequest request, Authentication authentication) { return communityService.updateComment(authentication.getName(), commentId, request); }
    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long commentId, Authentication authentication) { communityService.deleteComment(authentication.getName(), commentId); }
    @PostMapping("/comments/{commentId}/likes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void likeComment(@PathVariable Long commentId, Authentication authentication) { communityService.likeComment(authentication.getName(), commentId); }
    @DeleteMapping("/comments/{commentId}/likes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlikeComment(@PathVariable Long commentId, Authentication authentication) { communityService.unlikeComment(authentication.getName(), commentId); }
    @PostMapping("/files/presigned-url")
    public PresignedUploadResponse presigned(@Valid @RequestBody PresignedUploadRequest request) {
        S3FileService.UploadUrl upload = s3FileService.createUploadUrl(request.originalFileName(), request.contentType());
        return new PresignedUploadResponse(upload.key(), upload.url(), upload.expiresInSeconds());
    }
}
