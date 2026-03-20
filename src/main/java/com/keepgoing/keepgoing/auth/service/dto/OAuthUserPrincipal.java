package com.keepgoing.keepgoing.auth.service.dto;

import lombok.Builder;

@Builder
public record OAuthUserPrincipal(
		Long userId,
		String role
) {
}
