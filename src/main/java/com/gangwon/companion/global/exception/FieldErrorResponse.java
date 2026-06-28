package com.gangwon.companion.global.exception;

public record FieldErrorResponse(
        String field,
        String message,
        Object rejectedValue
) {
}
