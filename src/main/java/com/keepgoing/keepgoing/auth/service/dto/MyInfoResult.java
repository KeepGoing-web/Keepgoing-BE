package com.keepgoing.keepgoing.auth.service.dto;

import com.keepgoing.keepgoing.user.domain.UserRole;

public record MyInfoResult(
		Long userId,
		String email,
		String name,
		UserRole role
) {
}
