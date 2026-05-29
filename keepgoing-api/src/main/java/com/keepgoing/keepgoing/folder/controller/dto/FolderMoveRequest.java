package com.keepgoing.keepgoing.folder.controller.dto;

import com.keepgoing.keepgoing.folder.service.dto.FolderMoveCommand;
import com.keepgoing.keepgoing.global.validation.PositiveIfPresent;
import com.keepgoing.keepgoing.global.validation.PresentJsonNullable;
import jakarta.validation.valueextraction.Unwrapping;
import org.openapitools.jackson.nullable.JsonNullable;

public record FolderMoveRequest(
		@PresentJsonNullable(payload = {Unwrapping.Skip.class})
		@PositiveIfPresent(payload = {Unwrapping.Skip.class})
		JsonNullable<Long> parentId
) {

	public FolderMoveCommand toCommand(Long userId, Long folderId) {
		return new FolderMoveCommand(
				userId,
				folderId,
				parentId.orElse(null)
		);
	}
}
