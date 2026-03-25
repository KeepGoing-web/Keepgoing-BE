package com.keepgoing.keepgoing.folder.service.dto;

public record FolderSummaryResult(
		Long folderId,
		Long parentId,
		String name
) {

}
