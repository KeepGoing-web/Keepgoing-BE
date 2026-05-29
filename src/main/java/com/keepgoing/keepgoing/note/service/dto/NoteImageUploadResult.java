package com.keepgoing.keepgoing.note.service.dto;

import com.keepgoing.keepgoing.note.domain.NoteImageStatus;
import java.util.UUID;

public record NoteImageUploadResult(
		UUID publicId,
		NoteImageStatus status
) {
}
