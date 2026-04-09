package com.keepgoing.keepgoing.user.controller.dto;

import com.keepgoing.keepgoing.auth.validation.Password;
import com.keepgoing.keepgoing.user.service.dto.ChangePasswordCommand;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
		@NotBlank
		String currentPassword,

		@Password
		String newPassword
) {
	public ChangePasswordCommand toCommand(Long userId) {
		return new ChangePasswordCommand(
				userId,
				this.currentPassword,
				this.newPassword
		);
	}
}
