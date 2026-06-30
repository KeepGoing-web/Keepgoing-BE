package com.keepgoing.keepgoing.worker.application.dto;

public record SanitizedImage(
		byte[] bytes,
		String contentType,
		long fileSize
) {
	public SanitizedImage {
		if (bytes == null || bytes.length == 0) {
			throw new IllegalArgumentException("sanitized image bytes must not be empty");
		}
		if (contentType == null || contentType.isBlank()) {
			throw new IllegalArgumentException("sanitized image contentType must not be blank");
		}
		if (fileSize <= 0) {
			throw new IllegalArgumentException("sanitized image fileSize must be positive");
		}
	}
}
