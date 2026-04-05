package com.keepgoing.keepgoing.folder.controller.dto;

import com.keepgoing.keepgoing.folder.service.dto.FolderRenameCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FolderRenameRequest(
		@NotBlank
		@Size(max = 120)
		String name
) {
	public FolderRenameCommand toCommand(Long userId, Long folderId) {
		return new FolderRenameCommand(
				userId,
				folderId,
				name
		);
	}
}
