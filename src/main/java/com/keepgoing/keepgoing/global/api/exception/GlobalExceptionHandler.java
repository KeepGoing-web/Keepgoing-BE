package com.keepgoing.keepgoing.global.api.exception;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import com.keepgoing.keepgoing.global.api.response.ErrorResponse;
import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 예외 처리 (도메인에서 직접 던진 예외)
     *
     * @param ex
     * @return
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        ErrorDetail detail = ErrorDetail.of(
                errorCode,
                ex.getMessage()
        );
        ErrorResponse response = ErrorResponse.of(detail);

        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(response);
    }

    /**
     * @param ex
     * @return
     * @Valid 검증 실패 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex
    ) {
        List<ErrorDetail.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors().stream()
                .map(err -> new ErrorDetail.FieldError(
                        err.getField(),
                        err.getDefaultMessage()
                ))
                .toList();

        ErrorDetail detail = ErrorDetail.ofValidation(fieldErrors);
        ErrorResponse response = ErrorResponse.of(detail);

        return ResponseEntity.status(BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        ErrorDetail detail = ErrorDetail.of(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "알 수 없는 서버 에러가 발생했습니다."
        );
        ErrorResponse response = ErrorResponse.of(detail);

        return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                .body(response);
    }
}
