package com.keepgoing.keepgoing.global.api.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.keepgoing.keepgoing.global.api.exception.ErrorDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Schema(description = "에러 응답")
@RequiredArgsConstructor
@JsonPropertyOrder({"success", "error"})
public class ErrorResponse {

    @Schema(description = "성공 여부", example = "false")
    private final boolean success = false;

    @Schema(description = "에러 상세 정보")
    private final ErrorDetail error;

    public static ErrorResponse of(ErrorDetail error) {
        return new ErrorResponse(error);
    }
}
