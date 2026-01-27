package com.keepgoing.keepgoing.auth.service.dto;

public record LoginResult(
        String accessToken,
        String refreshToken,
        Long userId
) {
}
