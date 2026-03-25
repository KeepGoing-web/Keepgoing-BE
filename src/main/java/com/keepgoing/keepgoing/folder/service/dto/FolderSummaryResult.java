package com.keepgoing.keepgoing.folder.service.dto;

public record FolderResult(
		Long folderId,
		Long parentFolderId,
		String name
) {
}
