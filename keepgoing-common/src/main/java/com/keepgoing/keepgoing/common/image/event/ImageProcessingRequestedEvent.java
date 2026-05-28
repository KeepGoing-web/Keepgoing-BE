package com.keepgoing.keepgoing.common.image.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ImageProcessingRequestedEvent(
		UUID publicId,
		String storageKey,
		String contentType,
		long fileSize,
		Instant requestedAt
) {
	public Map<String, String> toMap() {
		return Map.of(
				"publicId", publicId.toString(),
				"storageKey", storageKey,
				"contentType", contentType,
				"fileSize", Long.toString(fileSize),
				"requestedAt", requestedAt.toString()
		);
	}

	public static ImageProcessingRequestedEvent from(Map<String, String> value) {
		return new ImageProcessingRequestedEvent(
				UUID.fromString(value.get("publicId")),
				value.get("storageKey"),
				value.get("contentType"),
				Long.parseLong(value.get("fileSize")),
				Instant.parse(value.get("requestedAt"))
		);
	}
}
