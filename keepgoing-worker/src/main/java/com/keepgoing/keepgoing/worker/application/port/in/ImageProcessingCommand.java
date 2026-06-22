package com.keepgoing.keepgoing.worker.application.port.in;

import java.time.Instant;
import java.util.UUID;

public record ImageProcessingCommand(
		UUID publicId,
		String storageKey,
		String contentType,
		long fileSize,
		Instant requestedAt
) {
}
