package com.gangwon.companion.domain.user.controller;

import com.gangwon.companion.domain.community.service.CommunityService;
import com.gangwon.companion.domain.community.dto.CommunityDtos.LikedCommentSummary;
import com.gangwon.companion.domain.community.dto.CommunityDtos.MyCommentSummary;
import com.gangwon.companion.domain.community.dto.CommunityDtos.PresignedUploadRequest;
import com.gangwon.companion.domain.community.dto.CommunityDtos.PresignedUploadResponse;
import com.gangwon.companion.domain.user.dto.response.MyPageResponse;
import com.gangwon.companion.domain.user.dto.request.NicknameChangeRequest;
import com.gangwon.companion.domain.user.dto.request.PasswordChangeRequest;
import com.gangwon.companion.domain.user.dto.request.ProfileImageChangeRequest;
import com.gangwon.companion.domain.user.dto.response.MyCommunityPostResponse;
import com.gangwon.companion.domain.user.dto.response.MyReviewResponse;
import com.gangwon.companion.domain.user.service.UserService;
import com.gangwon.companion.global.storage.S3FileService;
import com.gangwon.companion.global.web.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import java.util.List;

@Tag(name = "마이페이지", description = "로그인 사용자의 계정 및 여행 활동 정보 API")
@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class MyPageController {

    private final UserService userService;
    private final CommunityService communityService;
    private final S3FileService s3FileService;

    @Operation(summary = "마이페이지 조회")
    @GetMapping
    public ResponseEntity<MyPageResponse> getMyPage(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.getMyPage(userDetails.getUsername()));
    }

    @Operation(summary = "비밀번호 변경")
    @PatchMapping("/password")
    public ResponseEntity<MessageResponse> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PasswordChangeRequest request) {
        userService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(new MessageResponse("비밀번호가 변경되었습니다."));
    }

    @Operation(summary = "닉네임 변경")
    @PatchMapping("/nickname")
    public ResponseEntity<MessageResponse> changeNickname(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody NicknameChangeRequest request) {
        userService.changeNickname(userDetails.getUsername(), request);
        return ResponseEntity.ok(new MessageResponse("닉네임이 변경되었습니다."));
    }

    @Operation(summary = "프로필 이미지 변경")
    @PatchMapping("/profile-image")
    public ResponseEntity<MessageResponse> changeProfileImage(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ProfileImageChangeRequest request) {
        userService.changeProfileImage(userDetails.getUsername(), request);
        return ResponseEntity.ok(new MessageResponse("프로필 이미지가 변경되었습니다."));
    }

    @Operation(summary = "프로필 이미지 업로드 URL 발급")
    @PostMapping("/profile-image/presigned-url")
    public ResponseEntity<PresignedUploadResponse> createProfileImageUploadUrl(
            @Valid @RequestBody PresignedUploadRequest request) {
        S3FileService.UploadUrl upload = s3FileService.createUploadUrl("profiles", request.originalFileName(), request.contentType());
        return ResponseEntity.ok(new PresignedUploadResponse(upload.key(), upload.url(), upload.expiresInSeconds()));
    }

    @Operation(summary = "내가 작성한 커뮤니티 게시글 목록 조회")
    @GetMapping("/community/posts")
    public ResponseEntity<Page<MyCommunityPostResponse>> getMyCommunityPosts(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(communityService.myPosts(userDetails.getUsername(), pageable)
                .map(MyCommunityPostResponse::from));
    }

    @Operation(summary = "내가 좋아요한 커뮤니티 게시글 목록 조회")
    @GetMapping("/community/liked-posts")
    public ResponseEntity<Page<MyCommunityPostResponse>> getLikedCommunityPosts(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(communityService.likedPosts(userDetails.getUsername(), pageable)
                .map(MyCommunityPostResponse::from));
    }

    @Operation(summary = "내가 저장한 커뮤니티 게시글 목록 조회")
    @GetMapping("/community/saved-posts")
    public ResponseEntity<Page<MyCommunityPostResponse>> getSavedCommunityPosts(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(communityService.savedPosts(userDetails.getUsername(), pageable)
                .map(MyCommunityPostResponse::from));
    }

    @Operation(summary = "내가 작성한 커뮤니티 댓글 목록 조회")
    @GetMapping("/community/comments")
    public ResponseEntity<Page<MyCommentSummary>> getMyCommunityComments(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(communityService.myComments(userDetails.getUsername(), pageable));
    }

    @Operation(summary = "내가 좋아요한 커뮤니티 댓글 목록 조회")
    @GetMapping("/community/liked-comments")
    public ResponseEntity<Page<LikedCommentSummary>> getLikedCommunityComments(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(communityService.likedComments(userDetails.getUsername(), pageable));
    }

    @Operation(summary = "내가 작성한 리뷰 목록 조회")
    @GetMapping("/reviews")
    public ResponseEntity<List<MyReviewResponse>> getMyReviews(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.getMyReviews(userDetails.getUsername()));
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(
            @RequestHeader("Authorization") String authorization) {
        userService.logout(authorization.substring(7));
        return ResponseEntity.ok(new MessageResponse("로그아웃되었습니다."));
    }

    @Operation(summary = "회원탈퇴")
    @DeleteMapping
    public ResponseEntity<MessageResponse> withdraw(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("Authorization") String authorization) {
        userService.withdraw(userDetails.getUsername(), authorization.substring(7));
        return ResponseEntity.ok(new MessageResponse("회원탈퇴가 완료되었습니다."));
    }
}
