package com.keepgoing.keepgoing.note.service.dto;

import com.keepgoing.keepgoing.note.domain.Note;
import com.keepgoing.keepgoing.note.domain.NoteVisibility;
import java.time.LocalDateTime;

public record NoteDetailResult(
		Long noteId,
		Long folderId,
		Long userId,
		String title,
		String content,
		NoteVisibility visibility,
		boolean aiCollectable,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
	public static NoteDetailResult from(Note note) {
		return new NoteDetailResult(
				note.getId(),
				note.getFolder() != null ? note.getFolder().getId() : null,
				note.getAuthor().getId(),
				note.getTitle(),
				note.getContent(),
				note.getVisibility(),
				note.isAiCollectable(),
				note.getCreatedAt(),
				note.getUpdatedAt()
		);
	}
}
