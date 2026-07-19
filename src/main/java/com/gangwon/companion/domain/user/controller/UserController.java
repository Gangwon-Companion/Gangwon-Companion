package com.gangwon.companion.domain.user.controller;

import com.gangwon.companion.domain.user.dto.request.LoginRequest;
import com.gangwon.companion.domain.user.dto.request.SignUpRequest;
import com.gangwon.companion.domain.user.service.UserService;
import com.gangwon.companion.global.exception.ErrorResponse;
import com.gangwon.companion.global.web.AvailabilityResponse;
import com.gangwon.companion.global.web.MessageResponse;
import com.gangwon.companion.global.web.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증", description = "회원가입, 로그인, 중복 확인 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@SecurityRequirements
public class UserController {

    private final UserService userService;

    @Operation(summary = "회원가입")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "유효성 검사 실패 또는 중복 정보",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/signup")
    public ResponseEntity<MessageResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        userService.signUp(request);
        return ResponseEntity.ok(new MessageResponse("회원가입이 완료되었습니다."));
    }

    @Operation(summary = "로그인")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "아이디 또는 비밀번호 불일치",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = userService.login(request);
        return ResponseEntity.ok(new TokenResponse(token));
    }

    @Operation(summary = "아이디 중복 확인")
    @ApiResponse(responseCode = "200", description = "중복 여부 반환")
    @GetMapping("/check/username/{username}")
    public ResponseEntity<AvailabilityResponse> checkUsername(
            @Parameter(description = "확인할 아이디") @PathVariable String username) {
        boolean exists = userService.existsByUsername(username);
        return ResponseEntity.ok(new AvailabilityResponse(!exists));
    }

    @Operation(summary = "닉네임 중복 확인")
    @ApiResponse(responseCode = "200", description = "중복 여부 반환")
    @GetMapping("/check/nickname/{nickname}")
    public ResponseEntity<AvailabilityResponse> checkNickname(
            @Parameter(description = "확인할 닉네임") @PathVariable String nickname) {
        boolean exists = userService.existsByNickname(nickname);
        return ResponseEntity.ok(new AvailabilityResponse(!exists));
    }
}
