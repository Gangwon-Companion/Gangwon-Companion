package com.gangwon.companion.domain.user.dto.request;

import jakarta.validation.constraints.Size;

public record ProfileImageChangeRequest(
        @Size(max = 500)
        String profileImageS3Key
) {
}
