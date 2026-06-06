package com.keepgoing.keepgoing.common.image.event;

import com.keepgoing.keepgoing.common.image.domain.ImageProcessingStatus;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ImageProcessingResultEvent(
		UUID publicId,
		ImageProcessingStatus status,
		String reason,
		Instant processedAt,
		String secureStorageKey,
		String contentType,
		Long fileSize
) {
	public Map<String, String> toMap() {
		return Map.of(
				"publicId", publicId.toString(),
				"status", status.name(),
				"reason", reason == null ? "" : reason,
				"processedAt", processedAt.toString(),
				"secureStorageKey", secureStorageKey == null ? "" : secureStorageKey,
				"contentType", contentType == null ? "" : contentType,
				"fileSize", fileSize == null ? "" : fileSize.toString()
		);
	}

	public static ImageProcessingResultEvent scanning(UUID publicId, Instant processedAt) {
		return new ImageProcessingResultEvent(
				publicId,
				ImageProcessingStatus.SCANNING,
				"",
				processedAt,
				"",
				"",
				null
		);
	}

	public static ImageProcessingResultEvent rejected(UUID publicId, String reason, Instant processedAt) {
		return new ImageProcessingResultEvent(
				publicId,
				ImageProcessingStatus.REJECTED,
				reason,
				processedAt,
				"",
				"",
				null
		);
	}

	public static ImageProcessingResultEvent safe(
			UUID publicId,
			Instant processedAt,
			String secureStorageKey,
			String contentType,
			long fileSize
	) {
		return new ImageProcessingResultEvent(
				publicId,
				ImageProcessingStatus.SAFE,
				"",
				processedAt,
				secureStorageKey,
				contentType,
				fileSize
		);
	}

	public static ImageProcessingResultEvent from(Map<String, String> value) {
		return new ImageProcessingResultEvent(
				UUID.fromString(value.get("publicId")),
				ImageProcessingStatus.valueOf(value.get("status")),
				value.getOrDefault("reason", ""),
				Instant.parse(value.get("processedAt")),
				value.getOrDefault("secureStorageKey", ""),
				value.getOrDefault("contentType", ""),
				parseNullableLong(value.get("fileSize"))
		);
	}

	private static Long parseNullableLong(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return Long.parseLong(value);
	}
}
