package com.keepgoing.keepgoing.global.api.exception;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 클라이언트에게 전달할 에러 정보를 구조화하는 DTO
 *
 * @param code
 * @param message
 * @param fieldErrors
 */
@Schema(description = "에러 상세 정보")
@JsonInclude(Include.NON_NULL)
public record ErrorDetail(

        @Schema(description = "에러 코드", example = "AUTH_INVALID_CREDENTIALS")
        ErrorCode code,

        @Schema(description = "에러 메시지", example = "이메일 또는 비밀번호가 일치하지 않습니다.")
        String message,

        @Schema(
                description = "필드별 에러 목록 (주로 검증 오류 시 사용, 일반 비즈니스 에러는 null/빈 배열)",
                nullable = true
        )
        List<FieldError> fieldErrors
) {

    /**
     * 일반 비즈니스 에러용 (필드 단위 에러 없음)
     *
     * @param code
     * @param message
     * @return
     */
    public static ErrorDetail of(ErrorCode code, String message) {
        return new ErrorDetail(code, message, null);
    }

    /**
     * 검증 에러용 (여러 필드 에러 발생 가능)
     *
     * @param fieldErrors
     * @return
     */
    public static ErrorDetail ofValidation(List<FieldError> fieldErrors) {
        ErrorCode code = ErrorCode.VALIDATION_FAILED;
        return new ErrorDetail(
                code,
                code.getDefaultMessage(),
                fieldErrors
        );
    }

    @Schema(description = "필드명 에러 정보")
    public record FieldError(

            @Schema(description = "필드명", example = "email")
            String field,

            @Schema(description = "필드 에러 메시지", example = "이메일 형식이 올바르지 않습니다.")
            String message
    ) {
    }
}