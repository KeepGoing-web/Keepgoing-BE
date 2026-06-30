package com.keepgoing.keepgoing.note.service.dto;

import java.util.UUID;

public record NoteImageDeleteCommand(
		Long userId,
		Long noteId,
		UUID publicId
) {
}
