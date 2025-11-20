package com.keepgoing.keepgoing.global.api.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "성공 응답")
public class ApiResponse<T> extends BaseResponse {

    @Schema(description = "응답 데이터")
    private final T data;

    protected ApiResponse(T data) {
        super(true);
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data);
    }
}
