package com.keepgoing.keepgoing.note.controller.dto.response;

import com.keepgoing.keepgoing.note.domain.NoteImageStatus;
import com.keepgoing.keepgoing.note.service.dto.NoteImageUploadResult;

public record NoteImageUploadResponse(
		String publicId,
		NoteImageStatus status
) {
	public static NoteImageUploadResponse from(NoteImageUploadResult result) {
		return new NoteImageUploadResponse(
				result.publicId().toString(),
				result.status()
		);
	}
}
