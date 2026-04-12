package com.keepgoing.keepgoing.note.controller.dto;

import com.keepgoing.keepgoing.note.service.dto.NoteRenameCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NoteRenameRequest(
		@NotBlank
		@Size(max = 200)
		String title
) {

	public NoteRenameCommand toCommand(Long noteId, Long userId) {
		return new NoteRenameCommand(
				noteId,
				userId,
				this.title
		);
	}
}
