package com.keepgoing.keepgoing.note.service.dto;

import com.keepgoing.keepgoing.note.domain.NoteVisibility;

import java.time.LocalDateTime;

public record NoteDetailResult(
		Long noteId,
		Long userId,
		String title,
		String content,
		NoteVisibility visibility,
		boolean aiCollectable,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
