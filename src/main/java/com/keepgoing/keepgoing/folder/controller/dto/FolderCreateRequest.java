package com.keepgoing.keepgoing.folder.controller.dto;

import com.keepgoing.keepgoing.folder.service.dto.FolderCreateCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FolderCreateRequest(
		Long parentId,

		@NotBlank
		@Size(max = 120)
		String name
) {
	public FolderCreateCommand toCommand(Long userId) {
		return new FolderCreateCommand(
				userId,
				this.parentId,
				this.name
		);
	}
}
