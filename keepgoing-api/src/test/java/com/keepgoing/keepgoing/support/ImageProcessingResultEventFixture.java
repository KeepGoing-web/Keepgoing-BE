package com.keepgoing.keepgoing.support;

import com.keepgoing.keepgoing.common.image.domain.ImageProcessingStatus;
import com.keepgoing.keepgoing.common.image.event.ImageProcessingResultEvent;
import java.time.Instant;
import java.util.UUID;

public final class ImageProcessingResultEventFixture {

	public static final Instant PROCESSED_AT = Instant.parse("2026-05-15T00:01:00Z");
	public static final String SECURE_STORAGE_KEY = "notes/10/generated-image-key";
	public static final String CONTENT_TYPE = "image/png";
	public static final long FILE_SIZE = 1024L;

	private ImageProcessingResultEventFixture() {
	}

	public static ImageProcessingResultEvent scanningEvent(UUID publicId) {
		return ImageProcessingResultEvent.scanning(publicId, PROCESSED_AT);
	}

	public static ImageProcessingResultEvent safeEvent(UUID publicId) {
		return ImageProcessingResultEvent.safe(
				publicId,
				PROCESSED_AT,
				SECURE_STORAGE_KEY,
				CONTENT_TYPE,
				FILE_SIZE
		);
	}

	public static ImageProcessingResultEvent rejectedEvent(UUID publicId, String reason) {
		return ImageProcessingResultEvent.rejected(publicId, reason, PROCESSED_AT);
	}

	public static ImageProcessingResultEvent pendingEvent(UUID publicId) {
		return new ImageProcessingResultEvent(
				publicId,
				ImageProcessingStatus.PENDING,
				"",
				PROCESSED_AT,
				"",
				"",
				null
		);
	}
}
