package com.keepgoing.keepgoing.worker.infrastructure.adapter.in.redis;

import static org.assertj.core.api.Assertions.assertThat;

import com.keepgoing.keepgoing.common.image.event.ImageProcessingRequestedEvent;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ImageProcessingRequestMapperTest {

	@Test
	@DisplayName("이미지 처리 요청 이벤트를 use case command로 변환한다.")
	void mapsRequestedEventToCommand() {
		// given
		ImageProcessingRequestedEvent event = new ImageProcessingRequestedEvent(
				UUID.randomUUID(),
				"notes/10/generated-image-key",
				"image/png",
				1024L,
				Instant.parse("2026-05-15T00:00:00Z"),
				0
		);

		// when
		var command = ImageProcessingRequestMapper.toCommand(event);

		// then
		assertThat(command.publicId()).isEqualTo(event.publicId());
		assertThat(command.storageKey()).isEqualTo(event.storageKey());
		assertThat(command.contentType()).isEqualTo(event.contentType());
		assertThat(command.fileSize()).isEqualTo(event.fileSize());
		assertThat(command.requestedAt()).isEqualTo(event.requestedAt());
	}
}