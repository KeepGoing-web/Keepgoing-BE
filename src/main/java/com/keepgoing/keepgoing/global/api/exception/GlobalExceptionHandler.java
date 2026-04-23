package com.keepgoing.keepgoing.global.api.exception;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import com.keepgoing.keepgoing.global.api.response.ErrorResponse;
import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
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

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        ErrorCode errorCode = ErrorCode.AUTH_INVALID_CREDENTIALS;
        ErrorDetail detail = ErrorDetail.of(
                errorCode,
                errorCode.getDefaultMessage()
        );
        ErrorResponse response = ErrorResponse.of(detail);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(response);
    }

	@ExceptionHandler(NonTransientAiException.class)
	public ResponseEntity<ErrorResponse> handleNonTransientAi(NonTransientAiException ex) {
		// API 키 오류, 잘못된 요청 등 - 재시도 무의미
		ErrorDetail detail = ErrorDetail.of(
				ErrorCode.SERVICE_UNAVAILABLE,
				"AI 서비스 요청이 실패했습니다."
		);
		log.error("AI non-transient error", ex);
		ErrorResponse response = ErrorResponse.of(detail);
		return ResponseEntity.status(ErrorCode.SERVICE_UNAVAILABLE.getHttpStatus())
				.body(response);
	}

	@ExceptionHandler(TransientAiException.class)
	public ResponseEntity<ErrorResponse> handleTransientAi(TransientAiException ex) {
		// retry 모두 실패 후 도달 - 일시적 장애
		ErrorDetail detail = ErrorDetail.of(
				ErrorCode.SERVICE_UNAVAILABLE,
				"AI 서비스가 일시적으로 불안정합니다. 잠시 후 다시 시도해주세요."
		);
		log.warn("AI transient error after retries exhausted", ex);
		ErrorResponse response = ErrorResponse.of(detail);
		return ResponseEntity.status(ErrorCode.SERVICE_UNAVAILABLE.getHttpStatus())
				.body(response);
	}
}
