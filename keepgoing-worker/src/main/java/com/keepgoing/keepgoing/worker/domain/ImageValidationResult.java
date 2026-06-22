package com.keepgoing.keepgoing.worker.domain;

public record ImageValidationResult(
		boolean valid,
		String detectedContentType,
		ImageValidationFailureReason reason
) {
}
