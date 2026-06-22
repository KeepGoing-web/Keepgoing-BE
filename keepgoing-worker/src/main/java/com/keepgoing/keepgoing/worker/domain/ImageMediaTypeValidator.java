package com.keepgoing.keepgoing.worker.domain;

import com.keepgoing.keepgoing.common.image.domain.ImageContentTypePolicy;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.tika.Tika;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ImageMediaTypeValidator {

	private static final Tika TIKA = new Tika();

	public static ImageValidationResult validate(byte[] bytes, String requestedContentType) {
		String normalizedRequestedContentType = ImageContentTypePolicy.normalize(requestedContentType);
		String detectedContentType = ImageContentTypePolicy.normalize(detect(bytes));

		if (!ImageContentTypePolicy.isAllowed(normalizedRequestedContentType)) {
			return new ImageValidationResult(
					false,
					detectedContentType,
					ImageValidationFailureReason.UNSUPPORTED_CONTENT_TYPE
			);
		}

		if (!ImageContentTypePolicy.isAllowed(detectedContentType)) {
			return new ImageValidationResult(
					false,
					detectedContentType,
					ImageValidationFailureReason.UNSUPPORTED_IMAGE_SIGNATURE
			);
		}

		if (!detectedContentType.equals(normalizedRequestedContentType)) {
			return new ImageValidationResult(
					false,
					detectedContentType,
					ImageValidationFailureReason.CONTENT_TYPE_MISMATCH
			);
		}

		return new ImageValidationResult(
				true,
				detectedContentType,
				null
		);
	}

	private static String detect(byte[] bytes) {
		if (bytes == null || bytes.length == 0) {
			return null;
		}
		return TIKA.detect(bytes);
	}
}
