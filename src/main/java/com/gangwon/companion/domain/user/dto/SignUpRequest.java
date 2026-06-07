package com.gangwon.companion.domain.user.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;

@Getter
public class SignUpRequest {

    @NotBlank
    @Pattern(regexp = "^[a-z0-9]+$", message = "아이디는 영문 소문자와 숫자만 사용할 수 있습니다.")
    private String username;

    @NotBlank
    @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다.")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).+$",
            message = "비밀번호는 대문자, 소문자, 숫자, 특수기호를 각각 1개 이상 포함해야 합니다.")
    private String password;

    @NotBlank
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Pattern(regexp = "^[a-z0-9@.]+$", message = "이메일은 영문 소문자와 숫자만 사용할 수 있습니다.")
    private String email;

    @NotBlank
    @Size(max = 6, message = "닉네임은 최대 6글자까지 입력 가능합니다.")
    private String nickname;
}