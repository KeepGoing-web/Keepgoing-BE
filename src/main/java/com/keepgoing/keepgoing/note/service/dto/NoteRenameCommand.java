package com.keepgoing.keepgoing.note.service.dto;

public record NoteRenameCommand(
	Long noteId,
	Long userId,
	String title
) {
}

