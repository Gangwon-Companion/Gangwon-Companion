package com.gangwon.companion.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class LoginRequest {

    @Schema(description = "아이디", example = "testuser1")
    @NotBlank
    private String username;

    @Schema(description = "비밀번호", example = "Test1234!")
    @NotBlank
    private String password;
}