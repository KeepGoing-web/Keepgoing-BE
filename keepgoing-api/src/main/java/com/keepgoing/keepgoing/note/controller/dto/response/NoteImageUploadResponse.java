package com.keepgoing.keepgoing.note.controller.dto.response;

import com.keepgoing.keepgoing.common.image.domain.ImageProcessingStatus;
import com.keepgoing.keepgoing.note.service.dto.NoteImageUploadResult;

public record NoteImageUploadResponse(
		String publicId,
		ImageProcessingStatus status
) {
	public static NoteImageUploadResponse from(NoteImageUploadResult result) {
		return new NoteImageUploadResponse(
				result.publicId().toString(),
				result.status()
		);
	}
}
