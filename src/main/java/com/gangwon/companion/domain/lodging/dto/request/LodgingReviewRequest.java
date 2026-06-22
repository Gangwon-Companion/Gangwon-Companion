package com.gangwon.companion.domain.lodging.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LodgingReviewRequest(
        @NotBlank(message = "내용을 입력해주세요.")
        String content,

        @NotNull(message = "평점을 입력해주세요.")
        @DecimalMin(value = "0.0", message = "평점은 0.0 이상이어야 합니다.")
        @DecimalMax(value = "5.0", message = "평점은 5.0 이하이어야 합니다.")
        Double rating
) {
}