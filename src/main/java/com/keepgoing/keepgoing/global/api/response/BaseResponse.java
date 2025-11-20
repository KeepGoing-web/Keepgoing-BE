package com.keepgoing.keepgoing.global.api.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Schema(description = "공통 응답 베이스")
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseResponse {

    @Schema(description = "요청 여부", example = "true")
    private final boolean success;
}
