package com.keepgoing.keepgoing.worker.image.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;

import com.keepgoing.keepgoing.common.image.domain.ImageProcessingStatus;
import com.keepgoing.keepgoing.common.image.event.ImageProcessingResultEvent;
import com.keepgoing.keepgoing.worker.image.application.port.in.ImageProcessingCommand;
import com.keepgoing.keepgoing.worker.image.application.port.out.ImageProcessingResultPublisherPort;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImageProcessingServiceTest {

	private static final Instant REQUESTED_AT = Instant.parse("2026-05-15T00:00:00Z");
	private static final Instant PROCESSED_AT = Instant.parse("2026-05-15T00:01:00Z");

	@Mock
	ImageProcessingResultPublisherPort resultPublisher;

	ImageProcessingService imageProcessingService;

	@BeforeEach
	void setup() {
		imageProcessingService = new ImageProcessingService(
				Clock.fixed(PROCESSED_AT, ZoneOffset.UTC),
				resultPublisher
		);
	}

	@Test
	@DisplayName("이미지 처리 요청을 받으면 SCANNING과 SAFE 결과를 순서대로 발행한다.")
	void publishesScanningAndSafeResultsInOrder() {
		// given
		ImageProcessingCommand command = command();

		// when
		imageProcessingService.process(command);

		// then
		ArgumentCaptor<ImageProcessingResultEvent> eventCaptor
				= ArgumentCaptor.forClass(ImageProcessingResultEvent.class);

		InOrder inOrder = inOrder(resultPublisher);
		inOrder.verify(resultPublisher, times(2)).publish(eventCaptor.capture());

		assertThat(eventCaptor.getAllValues())
				.extracting(ImageProcessingResultEvent::status)
				.containsExactly(
						ImageProcessingStatus.SCANNING,
						ImageProcessingStatus.SAFE
				);

		assertThat(eventCaptor.getAllValues())
				.allSatisfy(event -> {
					assertThat(event.publicId()).isEqualTo(command.publicId());
					assertThat(event.reason()).isEmpty();
					assertThat(event.processedAt()).isEqualTo(PROCESSED_AT);
				});
	}

	private static ImageProcessingCommand command() {
		return new ImageProcessingCommand(
				UUID.randomUUID(),
				"notes/10/generated-image-key",
				"image/png",
				1024L,
				REQUESTED_AT
		);
	}
}