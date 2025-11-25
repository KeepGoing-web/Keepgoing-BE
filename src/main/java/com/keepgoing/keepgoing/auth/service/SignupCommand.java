package com.keepgoing.keepgoing.auth.service;

import lombok.Builder;

@Builder
public record SignupCommand(
        String email,
        String rawPassword,
        String name
) {
}
