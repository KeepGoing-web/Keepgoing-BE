package com.keepgoing.keepgoing.folder.controller.dto;

import com.keepgoing.keepgoing.folder.service.dto.CreateFolderCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateFolderRequest(
		Long parentId,

		@NotBlank
		@Size(max = 120)
		String name
) {
	public CreateFolderRequest {
		if (name != null) {
			name = name.trim();
		}
	}

	public CreateFolderCommand toCommand(Long userId) {
		return new CreateFolderCommand(
				userId,
				this.parentId,
				this.name
		);
	}
}
