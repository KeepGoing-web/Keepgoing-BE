package com.keepgoing.keepgoing.user.service.dto;

import com.keepgoing.keepgoing.user.domain.UserRole;

public record UserInfoResult(
		Long userId,
		String email,
		String name,
		UserRole role
) {
}
