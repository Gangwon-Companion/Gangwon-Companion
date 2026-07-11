package com.gangwon.companion.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    THEME_NOT_FOUND(HttpStatus.NOT_FOUND, "THEME_NOT_FOUND", "테마를 찾을 수 없습니다."),
    DESTINATION_DETAIL_NOT_FOUND(HttpStatus.NOT_FOUND, "DESTINATION_DETAIL_NOT_FOUND", "장소 상세정보를 찾을 수 없습니다."),
    EXTERNAL_API_RATE_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "EXTERNAL_API_RATE_LIMIT", "외부 API 호출 한도를 초과했습니다."),
    EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY, "EXTERNAL_API_ERROR", "외부 API 호출 중 오류가 발생했습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "잘못된 요청입니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "LOGIN_FAILED", "아이디 또는 비밀번호가 올바르지 않습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
