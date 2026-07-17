package com.gangwon.companion.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NicknameChangeRequest(
        @NotBlank @Size(max = 6) String nickname
) {
}
