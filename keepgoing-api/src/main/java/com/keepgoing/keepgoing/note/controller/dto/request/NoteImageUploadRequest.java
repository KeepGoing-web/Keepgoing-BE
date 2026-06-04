package com.keepgoing.keepgoing.note.controller.dto.request;

import com.keepgoing.keepgoing.common.image.domain.ImageContentTypePolicy;
import com.keepgoing.keepgoing.note.controller.validation.ValidImageFile;
import com.keepgoing.keepgoing.note.service.dto.NoteImageUploadCommand;
import jakarta.validation.constraints.NotNull;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

public record NoteImageUploadRequest(
		@NotNull
		@ValidImageFile
		MultipartFile file
) {
	public NoteImageUploadCommand toCommand(Long noteId) {
		return new NoteImageUploadCommand(
				noteId,
				file::getInputStream,
				StringUtils.getFilename(file.getOriginalFilename()),
				ImageContentTypePolicy.normalize(file.getContentType()),
				file.getSize()
		);
	}
}
