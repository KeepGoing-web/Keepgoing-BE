package com.keepgoing.keepgoing.worker.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.keepgoing.keepgoing.worker.application.port.in.ImageProcessingCommand;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ClaimedPendingMessageTest {

	@Nested
	@DisplayName("incrementRetryCount")
	class IncrementRetryCountTest {
		@Test
		@DisplayName("incrementRetryCount를 호출하면 retryCount가 1 증가한다")
		void incrementRetryCount_increasesByOne() {
			// given
			ClaimedPendingMessage message = new ClaimedPendingMessage(
					"msg1",
					UUID.randomUUID(),
					"key",
					"image/jpeg",
					1L,
					Instant.now(),
					0);

			// when
			message.incrementRetryCount();

			// then
			assertThat(message.getRetryCount()).isEqualTo(1);
		}

		@Test
		@DisplayName("incrementRetryCount를 여러 번 호출하면 누적 증가한다")
		void incrementRetryCount_accumulates() {
			// given
			ClaimedPendingMessage message = new ClaimedPendingMessage(
					"msg1",
					UUID.randomUUID(),
					"key",
					"image/jpeg",
					1L,
					Instant.now(),
					0);

			// when
			message.incrementRetryCount();
			message.incrementRetryCount();

			// then
			assertThat(message.getRetryCount()).isEqualTo(2);
		}
	}

	@Nested
	@DisplayName("isRetryExhausted")
	class IsRetryExhaustedTest {
		@Test
		@DisplayName("retryCount가 maxRetries 미만이면 false를 반환한다")
		void isRetryExhausted_underThreshold_returnsFalse() {
			// given
			ClaimedPendingMessage message = new ClaimedPendingMessage(
					"msg1",
					UUID.randomUUID(),
					"key",
					"image/jpeg",
					1L,
					Instant.now(),
					0);

			// when
			boolean retryExhausted = message.isRetryExhausted(3);

			// then
			assertThat(retryExhausted).isFalse();
		}

		@Test
		@DisplayName("retryCount가 maxRetries와 같으면 true를 반환한다")
		void isRetryExhausted_atThreshold_returnsTrue() {
			// given
			ClaimedPendingMessage message = new ClaimedPendingMessage(
					"msg1",
					UUID.randomUUID(),
					"key",
					"image/jpeg",
					1L,
					Instant.now(),
					3);

			// when
			boolean retryExhausted = message.isRetryExhausted(3);

			// then
			assertThat(retryExhausted).isTrue();
		}

		@Test
		@DisplayName("retryCount가 maxRetries를 초과하면 true를 반환한다")
		void isRetryExhausted_aboveThreshold_returnsTrue() {
			// given
			ClaimedPendingMessage message = new ClaimedPendingMessage(
					"msg1",
					UUID.randomUUID(),
					"key",
					"image/jpeg",
					1L,
					Instant.now(),
					4);

			// when
			boolean retryExhausted = message.isRetryExhausted(3);

			// then
			assertThat(retryExhausted).isTrue();
		}
	}

	@Nested
	@DisplayName("toCommand")
	class toCommandTest {
		@Test
		@DisplayName("toCommand로 만든 ImageProcessingCommand가 모든 필드를 동일하게 매핑한다")
		void toCommand_mapsAllFieldsCorrectly() {
			// given
			ClaimedPendingMessage message = new ClaimedPendingMessage(
					"msg1",
					UUID.randomUUID(),
					"key",
					"image/jpeg",
					1L,
					Instant.now(),
					2);

			// when
			ImageProcessingCommand command = message.toCommand();

			// then
			assertThat(message.getPublicId()).isEqualTo(command.publicId());
			assertThat(message.getStorageKey()).isEqualTo(command.storageKey());
			assertThat(message.getContentType()).isEqualTo(command.contentType());
			assertThat(message.getFileSize()).isEqualTo(command.fileSize());
			assertThat(message.getRequestedAt()).isEqualTo(command.requestedAt());
			assertThat(message.getRetryCount()).isEqualTo(command.retryCount());
		}
	}
}