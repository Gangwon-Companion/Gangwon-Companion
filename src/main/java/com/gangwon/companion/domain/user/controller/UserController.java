package com.gangwon.companion.domain.user.controller;

import com.gangwon.companion.domain.user.dto.LoginRequest;
import com.gangwon.companion.domain.user.dto.SignUpRequest;
import com.gangwon.companion.domain.user.service.UserService;
import com.gangwon.companion.global.web.AvailabilityResponse;
import com.gangwon.companion.global.web.MessageResponse;
import com.gangwon.companion.global.web.TokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<MessageResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        userService.signUp(request);
        return ResponseEntity.ok(new MessageResponse("회원가입이 완료되었습니다."));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = userService.login(request);
        return ResponseEntity.ok(new TokenResponse(token));
    }

    @GetMapping("/check/username/{username}")
    public ResponseEntity<AvailabilityResponse> checkUsername(@PathVariable String username) {
        boolean exists = userService.existsByUsername(username);
        return ResponseEntity.ok(new AvailabilityResponse(!exists));
    }

    @GetMapping("/check/nickname/{nickname}")
    public ResponseEntity<AvailabilityResponse> checkNickname(@PathVariable String nickname) {
        boolean exists = userService.existsByNickname(nickname);
        return ResponseEntity.ok(new AvailabilityResponse(!exists));
    }
}
