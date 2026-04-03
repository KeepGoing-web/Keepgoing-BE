package com.keepgoing.keepgoing.note.service.dto;

public record NoteMoveCommand(
		Long noteId,
		Long userId,
		Long folderId
) {
}
