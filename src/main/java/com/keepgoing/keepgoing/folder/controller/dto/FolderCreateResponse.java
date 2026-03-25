package com.keepgoing.keepgoing.folder.controller.dto;

import com.keepgoing.keepgoing.folder.service.dto.FolderSummaryResult;

public record CreateFolderResponse(
		Long folderId,
		Long parentFolderId,
		String name
) {

	public static CreateFolderResponse from(FolderSummaryResult result) {
		return new CreateFolderResponse(
				result.folderId(),
				result.parentFolderId(),
				result.name()
		);
	}
}
