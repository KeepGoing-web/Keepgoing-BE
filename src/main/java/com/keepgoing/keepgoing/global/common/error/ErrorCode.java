package com.keepgoing.keepgoing.global.common.error;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "에러 코드")
public enum ErrorCode {

    // Auth
    AUTH_INVALID_CREDENTIALS,
    AUTH_TOKEN_EXPIRED,
    AUTH_TOKEN_INVALID,
    AUTH_TOKEN_MISSING,
    AUTH_REFRESH_TOKEN_INVALID,
    AUTH_REFRESH_TOKEN_EXPIRED,
    AUTH_EMAIL_NOT_VERIFIED,

    // User
    USER_NOT_FOUND,
    USER_ALREADY_EXISTS,
    USER_SUSPENDED,
    USER_DELETED,

    // Validation
    VALIDATION_FAILED,
    INVALID_INPUT,
    INVALID_PASSWORD_FORMAT,
    PASSWORD_TOO_WEAK,

    // Permission
    PERMISSION_DENIED,
    ROLE_INSUFFICIENT,

    // Rate limit
    RATE_LIMIT_EXCEEDED,

    // System
    INTERNAL_SERVER_ERROR,
    SERVICE_UNAVAILABLE,
    RESOURCE_NOT_FOUND
}
