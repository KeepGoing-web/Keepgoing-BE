package com.keepgoing.keepgoing.folder.controller.dto;

import com.keepgoing.keepgoing.folder.service.dto.FolderResult;

public record CreateFolderResponse(
		Long folderId,
		Long parentFolderId,
		String name
) {

	public static CreateFolderResponse from(FolderResult result) {
		return new CreateFolderResponse(
				result.folderId(),
				result.parentFolderId(),
				result.name()
		);
	}
}
