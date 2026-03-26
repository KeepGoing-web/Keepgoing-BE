package com.keepgoing.keepgoing.folder.controller.dto;

import com.keepgoing.keepgoing.folder.service.dto.FolderSummaryResult;

public record FolderCreateResponse(
		Long folderId,
		Long parentId,
		String name
) {

	public static FolderCreateResponse from(FolderSummaryResult result) {
		return new FolderCreateResponse(
				result.folderId(),
				result.parentId(),
				result.name()
		);
	}
}
