package com.keepgoing.keepgoing.folder.repository.dto;

public record FolderTreeRow(
		Long folderId,
		Long parentId,
		String name) {
}