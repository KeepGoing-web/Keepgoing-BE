package com.keepgoing.keepgoing.user.service.dto;

public record UserUpdateCommand(
		Long userId,
		String name
) {
}
