package com.keepgoing.keepgoing.user.controller.dto;

import com.keepgoing.keepgoing.user.service.dto.UserUpdateCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
		@NotBlank
		@Size(max = 100)
		String name
) {
	public UserUpdateRequest {
		name = (name == null) ? null : name.trim();
	}

	public UserUpdateCommand toCommand(Long userId) {
		return new UserUpdateCommand(
				userId,
				name
		);
	}
}
