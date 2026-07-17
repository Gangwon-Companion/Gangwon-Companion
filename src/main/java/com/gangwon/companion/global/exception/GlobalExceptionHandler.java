package com.gangwon.companion.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e, HttpServletRequest request) {
        return buildResponse(e.getErrorCode(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e,
                                                               HttpServletRequest request) {
        return buildResponse(ErrorCode.INVALID_REQUEST, e.getMessage(), request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException e,
                                                              HttpServletRequest request) {
        return buildResponse(ErrorCode.BAD_CREDENTIALS, request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException e,
                                                             HttpServletRequest request) {
        return buildResponse(ErrorCode.UNAUTHORIZED, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e,
                                                            HttpServletRequest request) {
        return buildResponse(ErrorCode.ACCESS_DENIED, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
                                                                     HttpServletRequest request) {
        List<FieldErrorResponse> errors = e.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldErrorResponse)
                .toList();
        return buildResponse(ErrorCode.VALIDATION_FAILED, request, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e,
                                                                  HttpServletRequest request) {
        List<FieldErrorResponse> errors = e.getConstraintViolations().stream()
                .map(violation -> new FieldErrorResponse(
                        violation.getPropertyPath().toString(),
                        violation.getMessage(),
                        violation.getInvalidValue()
                ))
                .toList();
        return buildResponse(ErrorCode.VALIDATION_FAILED, request, errors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e,
                                                           HttpServletRequest request) {
        String message = ErrorCode.TYPE_MISMATCH.getMessage() + ": " + e.getName();
        return buildResponse(ErrorCode.TYPE_MISMATCH, message, request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestParameter(MissingServletRequestParameterException e,
                                                                      HttpServletRequest request) {
        String message = ErrorCode.MISSING_PARAMETER.getMessage() + ": " + e.getParameterName();
        return buildResponse(ErrorCode.MISSING_PARAMETER, message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException e,
                                                                     HttpServletRequest request) {
        return buildResponse(ErrorCode.MALFORMED_JSON, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e,
                                                                 HttpServletRequest request) {
        return buildResponse(ErrorCode.METHOD_NOT_ALLOWED, request);
    }

    @ExceptionHandler(HttpClientErrorException.TooManyRequests.class)
    public ResponseEntity<ErrorResponse> handleExternalApiRateLimit(
            HttpClientErrorException.TooManyRequests e,
            HttpServletRequest request
    ) {
        return buildResponse(ErrorCode.EXTERNAL_API_RATE_LIMIT, request);
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ErrorResponse> handleExternalApiException(
            RestClientException e,
            HttpServletRequest request
    ) {
        return buildResponse(ErrorCode.EXTERNAL_API_ERROR, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException e,
                                                                     HttpServletRequest request) {
        return buildResponse(ErrorCode.DATA_INTEGRITY_VIOLATION, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
        return buildResponse(ErrorCode.INTERNAL_SERVER_ERROR, request);
    }

    private FieldErrorResponse toFieldErrorResponse(FieldError fieldError) {
        return new FieldErrorResponse(
                fieldError.getField(),
                fieldError.getDefaultMessage(),
                isSensitiveField(fieldError.getField()) ? null : fieldError.getRejectedValue()
        );
    }

    private boolean isSensitiveField(String fieldName) {
        return fieldName != null && fieldName.toLowerCase().contains("password");
    }

    private ResponseEntity<ErrorResponse> buildResponse(ErrorCode errorCode, HttpServletRequest request) {
        return buildResponse(errorCode, errorCode.getMessage(), request);
    }

    private ResponseEntity<ErrorResponse> buildResponse(ErrorCode errorCode, String message,
                                                       HttpServletRequest request) {
        return buildResponse(errorCode, message, request, List.of());
    }

    private ResponseEntity<ErrorResponse> buildResponse(ErrorCode errorCode, HttpServletRequest request,
                                                       List<FieldErrorResponse> errors) {
        return buildResponse(errorCode, errorCode.getMessage(), request, errors);
    }

    private ResponseEntity<ErrorResponse> buildResponse(ErrorCode errorCode, String message,
                                                       HttpServletRequest request,
                                                       List<FieldErrorResponse> errors) {
        HttpStatus status = errorCode.getStatus();
        ErrorResponse response = ErrorResponse.of(
                errorCode.getCode(),
                status.value(),
                message,
                request.getRequestURI(),
                errors
        );
        return ResponseEntity.status(status).body(response);
    }
}
