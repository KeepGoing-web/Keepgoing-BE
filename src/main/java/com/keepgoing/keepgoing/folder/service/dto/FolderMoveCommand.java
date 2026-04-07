package com.keepgoing.keepgoing.folder.service.dto;

public record FolderMoveCommand(
		Long userId,
		Long folderId,
		Long parentId
) {
}
