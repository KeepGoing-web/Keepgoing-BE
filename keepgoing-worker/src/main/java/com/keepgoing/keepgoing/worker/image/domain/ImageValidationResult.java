package com.keepgoing.keepgoing.worker.image.domain;

public record ImageValidationResult(
		boolean valid,
		String detectedContentType,
		ImageValidationFailureReason reason
) {
}
