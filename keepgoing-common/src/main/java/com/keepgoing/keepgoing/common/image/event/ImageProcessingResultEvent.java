package com.keepgoing.keepgoing.common.image.event;

import com.keepgoing.keepgoing.common.image.domain.ImageProcessingStatus;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ImageProcessingResultEvent(
		UUID publicId,
		ImageProcessingStatus status,
		String reason,
		Instant processedAt
) {
	public Map<String, String> toMap() {
		return Map.of(
				"publicId", publicId.toString(),
				"status", status.name(),
				"reason", reason == null ? "" : reason,
				"processedAt", processedAt.toString()
		);
	}

	public static ImageProcessingResultEvent from(Map<String, String> value) {
		return new ImageProcessingResultEvent(
				UUID.fromString(value.get("publicId")),
				ImageProcessingStatus.valueOf(value.get("status")),
				value.getOrDefault("reason", ""),
				Instant.parse(value.get("processedAt"))
		);
	}
}
