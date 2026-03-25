package com.keepgoing.keepgoing.folder.service.dto;

public record FolderCreateCommand(
		Long userId,
		Long parentFolderId,
		String name
) {
}
