package com.keepgoing.keepgoing.global.common.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;

    public BusinessException(ErrorCode errorCode, HttpStatus httpStatus, String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public BusinessException(ErrorCode errorCode, HttpStatus httpStatus) {
        super(errorCode != null ? errorCode.name() : null);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
