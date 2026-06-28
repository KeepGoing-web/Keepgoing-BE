package com.keepgoing.keepgoing.worker.domain;

import com.keepgoing.keepgoing.common.image.domain.ImageContentTypePolicy;

public class ImageMediaTypeValidator {

	public static ImageValidationResult validate(
			String requestedContentType,
			String detectedContentType
	) {
		String normalizedRequestedContentType
				= ImageContentTypePolicy.normalize(requestedContentType);
		String normalizedDetectedContentType
				= ImageContentTypePolicy.normalize(detectedContentType);

		if (!ImageContentTypePolicy.isAllowed(normalizedRequestedContentType)) {
			return new ImageValidationResult(
					false,
					normalizedDetectedContentType,
					ImageValidationFailureReason.UNSUPPORTED_CONTENT_TYPE
			);
		}

		if (!ImageContentTypePolicy.isAllowed(normalizedDetectedContentType)) {
			return new ImageValidationResult(
					false,
					normalizedDetectedContentType,
					ImageValidationFailureReason.UNSUPPORTED_IMAGE_SIGNATURE
			);
		}

		if (!normalizedDetectedContentType.equals(normalizedRequestedContentType)) {
			return new ImageValidationResult(
					false,
					normalizedDetectedContentType,
					ImageValidationFailureReason.CONTENT_TYPE_MISMATCH
			);
		}

		return new ImageValidationResult(
				true,
				normalizedDetectedContentType,
				null
		);
	}
}
