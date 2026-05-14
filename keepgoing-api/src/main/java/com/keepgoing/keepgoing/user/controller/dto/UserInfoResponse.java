package com.keepgoing.keepgoing.user.controller.dto;

import com.keepgoing.keepgoing.user.service.dto.UserInfoResult;
import com.keepgoing.keepgoing.user.domain.UserRole;

public record UserInfoResponse(
		Long userId,
		String email,
		String name,
		UserRole role
) {
	public static UserInfoResponse from(UserInfoResult dto) {
		return new UserInfoResponse(
				dto.userId(),
				dto.email(),
				dto.name(),
				dto.role()
		);
	}
}
