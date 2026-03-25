package com.keepgoing.keepgoing.folder.service.dto;

public record CreateFolderCommand(
		Long userId,
		Long parentFolderId,
		String name
) {
}
