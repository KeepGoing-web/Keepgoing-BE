package com.keepgoing.keepgoing.worker.application.dto;

import java.time.Instant;
import java.util.UUID;

public record PreValidatedImage(
		UUID publicId,
		String storageKey,
		String requestedContentType,
		String detectedContentType,
		long fileSize,
		Instant requestedAt
) {
}
