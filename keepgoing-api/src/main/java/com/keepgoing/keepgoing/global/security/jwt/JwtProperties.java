package com.keepgoing.keepgoing.global.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secretKey,
        Long accessTokenExpiry,
        Long refreshTokenExpiry,
        String issuer
) {
}
