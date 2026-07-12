package com.gangwon.companion.global.exception;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        String code,
        int status,
        String message,
        String path,
        Instant timestamp,
        List<FieldErrorResponse> errors
) {

    public static ErrorResponse of(String code, int status, String message, String path,
                                   List<FieldErrorResponse> errors) {
        return new ErrorResponse(code, status, message, path, Instant.now(), errors);
    }
}
