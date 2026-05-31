package com.keepgoing.keepgoing.note.service.dto;

import com.keepgoing.keepgoing.common.image.domain.ImageProcessingStatus;
import java.util.UUID;

public record NoteImageUploadResult(
		UUID publicId,
		ImageProcessingStatus status
) {
}
