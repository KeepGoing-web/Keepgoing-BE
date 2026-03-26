package com.keepgoing.keepgoing.note.service.dto;

import com.keepgoing.keepgoing.note.domain.Note;
import com.keepgoing.keepgoing.note.domain.NoteVisibility;

import java.time.LocalDateTime;

public record NoteSummaryResult(
		Long noteId,
		Long folderId,
		String title,
		NoteVisibility visibility,
		boolean aiCollectable,
		LocalDateTime createdAt
) {
	public static NoteSummaryResult from(Note note) {
		return new NoteSummaryResult(
				note.getId(),
				note.getFolder() != null ? note.getFolder().getId() : null,
				note.getTitle(),
				note.getVisibility(),
				note.isAiCollectable(),
				note.getCreatedAt()
		);
	}
}