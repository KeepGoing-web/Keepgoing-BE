package com.keepgoing.keepgoing.worker.image.application.service;

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
