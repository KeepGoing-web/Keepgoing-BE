package com.keepgoing.keepgoing.note.service.dto;

import com.keepgoing.keepgoing.global.storage.InputStreamSupplier;

public record NoteImageUploadCommand(
		Long noteId,
		InputStreamSupplier inputStreamSupplier,
		String originalFileName,
		String contentType,
		long filesize
) {
}
