package com.keepgoing.keepgoing.folder.service.dto;

public record FolderRenameCommand(
		Long userId,
		Long folderId,
		String name
) {
}
