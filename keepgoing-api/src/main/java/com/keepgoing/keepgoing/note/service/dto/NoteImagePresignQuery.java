package com.keepgoing.keepgoing.note.service.dto;

import java.util.UUID;

public record NoteImagePresignQuery(
		Long userId,
		Long noteId,
		UUID publicId
) {
}
