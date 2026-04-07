package com.keepgoing.keepgoing.user.service.dto;

public record ChangePasswordCommand(
		Long userId,
		String currentPassword,
		String newPassword
) {
}
