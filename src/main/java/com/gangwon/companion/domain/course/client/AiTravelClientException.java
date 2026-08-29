package com.gangwon.companion.domain.course.client;

import com.gangwon.companion.global.exception.BusinessException;
import com.gangwon.companion.global.exception.ErrorCode;

public class AiTravelClientException extends BusinessException {
    public AiTravelClientException(ErrorCode errorCode, Throwable cause) {
        super(errorCode);
        initCause(cause);
    }
}
