package com.gangwon.companion.domain.user.controller;

import com.gangwon.companion.domain.user.dto.response.MyPageResponse;
import com.gangwon.companion.domain.user.dto.request.NicknameChangeRequest;
import com.gangwon.companion.domain.user.dto.request.PasswordChangeRequest;
import com.gangwon.companion.domain.user.service.UserService;
import com.gangwon.companion.global.web.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@Tag(name = "마이페이지", description = "로그인 사용자의 계정 및 여행 활동 정보 API")
@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class MyPageController {

    private final UserService userService;

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

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(
            @RequestHeader("Authorization") String authorization) {
        userService.logout(authorization.substring(7));
        return ResponseEntity.ok(new MessageResponse("로그아웃되었습니다."));
    }
}
