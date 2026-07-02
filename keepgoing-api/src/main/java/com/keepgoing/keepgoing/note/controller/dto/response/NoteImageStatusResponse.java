package com.keepgoing.keepgoing.note.controller.dto.response;

import com.keepgoing.keepgoing.common.image.domain.ImageProcessingStatus;
import com.keepgoing.keepgoing.note.service.dto.NoteImageStatusResult;
import java.util.UUID;

public record NoteImageStatusResponse(
		UUID publicId,
		ImageProcessingStatus status
) {
	public static NoteImageStatusResponse from(NoteImageStatusResult result) {
		return new NoteImageStatusResponse(
				result.publicId(),
				result.status()
		);
	}
}
