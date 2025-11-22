package com.keepgoing.keepgoing.global.api.response;

import com.keepgoing.keepgoing.global.api.exception.ErrorDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "에러 응답")
public class ErrorResponse extends BaseResponse {

    @Schema(description = "에러 정보")
    private final ErrorDetail error;

    protected ErrorResponse(ErrorDetail error) {
        super(false);
        this.error = error;
    }

    public static ErrorResponse of(ErrorDetail error) {
        return new ErrorResponse(error);
    }
}
