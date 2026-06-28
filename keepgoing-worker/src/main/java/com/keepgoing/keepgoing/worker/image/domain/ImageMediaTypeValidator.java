package com.keepgoing.keepgoing.worker.image.domain;

import java.util.Locale;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.tika.Tika;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ImageMediaTypeValidator {

	private static final Tika TIKA = new Tika();
	private static final String IMAGE_PREFIX = "image/";

	public static ImageValidationResult validate(byte[] bytes, String requestedContentType) {
		String normalizedRequestedContentType = normalize(requestedContentType);
		String detectedContentType = detect(bytes);

		if (!isImageContentType(normalizedRequestedContentType)) {
			return new ImageValidationResult(
					false,
					detectedContentType,
					ImageValidationFailureReason.UNSUPPORTED_CONTENT_TYPE
			);
		}

		if (!isImageContentType(detectedContentType)) {
			return new ImageValidationResult(
					false,
					null,
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

	private static String normalize(String contentType) {
		if (contentType == null) {
			return null;
		}
		return contentType.trim().toLowerCase(Locale.ROOT);
	}

	private static boolean isImageContentType(String contentType) {
		return contentType != null && contentType.startsWith(IMAGE_PREFIX);
	}

	private static String detect(byte[] bytes) {
		if (bytes == null || bytes.length == 0) {
			return null;
		}
		return normalize(TIKA.detect(bytes));
	}
}
