package com.keepgoing.keepgoing.folder.service.dto;

import com.keepgoing.keepgoing.folder.domain.Folder;

public record FolderSummaryResult(
		Long folderId,
		Long parentId,
		String name
) {
	public static FolderSummaryResult from(Folder folder) {
		return new FolderSummaryResult(
				folder.getId(),
				folder.getParent() != null ? folder.getParent().getId() : null,
				folder.getName()
		);
	}
}
