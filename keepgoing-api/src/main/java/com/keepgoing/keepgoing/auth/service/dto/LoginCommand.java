package com.keepgoing.keepgoing.auth.service.dto;

public record LoginCommand(
        String email,
        String rawPassword
) {
}
