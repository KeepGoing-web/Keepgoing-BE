package com.keepgoing.keepgoing.auth.controller.dto;

import com.keepgoing.keepgoing.user.domain.UserRole;

public record MyInfoResponse(
		Long userId,
		String email,
		String name,
		UserRole role
) {
}
