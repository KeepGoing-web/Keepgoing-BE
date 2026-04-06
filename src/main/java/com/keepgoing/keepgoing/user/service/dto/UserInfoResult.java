package com.keepgoing.keepgoing.user.service.dto;

import com.keepgoing.keepgoing.user.domain.User;
import com.keepgoing.keepgoing.user.domain.UserRole;

public record UserInfoResult(
		Long userId,
		String email,
		String name,
		UserRole role
) {
	public static UserInfoResult from(User user) {
		return  new UserInfoResult(
				user.getId(),
				user.getEmail(),
				user.getName(),
				user.getRole()
		);
	}
}
