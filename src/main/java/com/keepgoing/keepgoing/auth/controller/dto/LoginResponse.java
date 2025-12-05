package com.keepgoing.keepgoing.auth.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 응답")
public record LoginResponse(
        String accessToken,
        String refreshToken,
        Long userId
) {
}
