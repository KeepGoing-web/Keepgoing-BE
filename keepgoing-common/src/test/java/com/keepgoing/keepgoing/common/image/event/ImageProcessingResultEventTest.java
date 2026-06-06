package com.keepgoing.keepgoing.common.image.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.keepgoing.keepgoing.common.image.domain.ImageProcessingStatus;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ImageProcessingResultEventTest {

	private static final UUID PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final Instant PROCESSED_AT = Instant.parse("2026-05-15T00:01:00Z");
	private static final String SECURE_STORAGE_KEY = "notes/10/generated-image-key";
	private static final String CONTENT_TYPE = "image/png";
	private static final long FILE_SIZE = 123L;

	@Test
	@DisplayName("SCANNING 이벤트는 처리 시작 상태와 공통 필드를 가진다")
	void createsScanningEvent() {
		// when
		ImageProcessingResultEvent event = ImageProcessingResultEvent.scanning(PUBLIC_ID, PROCESSED_AT);

		// then
		assertThat(event.publicId()).isEqualTo(PUBLIC_ID);
		assertThat(event.status()).isEqualTo(ImageProcessingStatus.SCANNING);
		assertThat(event.reason()).isEmpty();
		assertThat(event.processedAt()).isEqualTo(PROCESSED_AT);
		assertThat(event.secureStorageKey()).isEmpty();
		assertThat(event.contentType()).isEmpty();
		assertThat(event.fileSize()).isNull();
	}

	@Test
	@DisplayName("REJECTED 이벤트는 거부 상태와 실패 사유를 가진다")
	void createsRejectedEvent() {
		// when
		ImageProcessingResultEvent event = ImageProcessingResultEvent.rejected(
				PUBLIC_ID,
				"CONTENT_TYPE_MISMATCH",
				PROCESSED_AT
		);

		// then
		assertThat(event.publicId()).isEqualTo(PUBLIC_ID);
		assertThat(event.status()).isEqualTo(ImageProcessingStatus.REJECTED);
		assertThat(event.reason()).isEqualTo("CONTENT_TYPE_MISMATCH");
		assertThat(event.processedAt()).isEqualTo(PROCESSED_AT);
		assertThat(event.secureStorageKey()).isEmpty();
		assertThat(event.contentType()).isEmpty();
		assertThat(event.fileSize()).isNull();
	}

	@Test
	@DisplayName("SAFE 이벤트는 secure 저장 결과 정보를 가진다")
	void createsSafeEvent() {
		// when
		ImageProcessingResultEvent event = ImageProcessingResultEvent.safe(
				PUBLIC_ID,
				PROCESSED_AT,
				SECURE_STORAGE_KEY,
				CONTENT_TYPE,
				FILE_SIZE
		);

		// then
		assertThat(event.publicId()).isEqualTo(PUBLIC_ID);
		assertThat(event.status()).isEqualTo(ImageProcessingStatus.SAFE);
		assertThat(event.reason()).isEmpty();
		assertThat(event.processedAt()).isEqualTo(PROCESSED_AT);
		assertThat(event.secureStorageKey()).isEqualTo(SECURE_STORAGE_KEY);
		assertThat(event.contentType()).isEqualTo(CONTENT_TYPE);
		assertThat(event.fileSize()).isEqualTo(FILE_SIZE);
	}

	@Test
	@DisplayName("이벤트를 Redis Stream 값으로 변환하면 secure 결과 필드를 포함한다")
	void toMapIncludesSecureResultFields() {
		// given
		ImageProcessingResultEvent event = ImageProcessingResultEvent.safe(
				PUBLIC_ID,
				PROCESSED_AT,
				SECURE_STORAGE_KEY,
				CONTENT_TYPE,
				FILE_SIZE
		);

		// when
		Map<String, String> value = event.toMap();

		// then
		assertThat(value).containsAllEntriesOf(Map.of(
				"publicId", PUBLIC_ID.toString(),
				"status", ImageProcessingStatus.SAFE.name(),
				"reason", "",
				"processedAt", PROCESSED_AT.toString(),
				"secureStorageKey", SECURE_STORAGE_KEY,
				"contentType", CONTENT_TYPE,
				"fileSize", String.valueOf(FILE_SIZE)
		));
	}

	@Test
	@DisplayName("secure 결과 필드가 있는 Redis Stream 값을 이벤트로 복원한다")
	void fromRestoresEventWithSecureResultFields() {
		// given
		Map<String, String> value = Map.of(
				"publicId", PUBLIC_ID.toString(),
				"status", ImageProcessingStatus.SAFE.name(),
				"reason", "",
				"processedAt", PROCESSED_AT.toString(),
				"secureStorageKey", SECURE_STORAGE_KEY,
				"contentType", CONTENT_TYPE,
				"fileSize", String.valueOf(FILE_SIZE)
		);

		// when
		ImageProcessingResultEvent event = ImageProcessingResultEvent.from(value);

		// then
		assertThat(event).isEqualTo(ImageProcessingResultEvent.safe(
				PUBLIC_ID,
				PROCESSED_AT,
				SECURE_STORAGE_KEY,
				CONTENT_TYPE,
				FILE_SIZE
		));
	}

	@Test
	@DisplayName("secure 결과 필드가 없는 기존 Redis Stream 값도 이벤트로 복원한다")
	void fromRestoresLegacyEventWithoutSecureResultFields() {
		// given
		Map<String, String> value = Map.of(
				"publicId", PUBLIC_ID.toString(),
				"status", ImageProcessingStatus.SCANNING.name(),
				"reason", "",
				"processedAt", PROCESSED_AT.toString()
		);

		// when
		ImageProcessingResultEvent event = ImageProcessingResultEvent.from(value);

		// then
		assertThat(event).isEqualTo(ImageProcessingResultEvent.scanning(PUBLIC_ID, PROCESSED_AT));
	}
}
