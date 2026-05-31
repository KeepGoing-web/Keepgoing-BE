package com.keepgoing.keepgoing.global.api.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Schema(description = "성공 응답")
@RequiredArgsConstructor
@JsonPropertyOrder({"success", "data"})
public class ApiResponse<T> {

    @Schema(description = "성공 여부", example = "true")
    private final boolean success = true;

    @Schema(description = "응답 데이터")
    private final T data;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data);
    }
}
