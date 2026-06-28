package com.keepgoing.keepgoing.worker.image.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.keepgoing.keepgoing.common.image.domain.ImageProcessingStatus;
import com.keepgoing.keepgoing.common.image.event.ImageProcessingResultEvent;
import com.keepgoing.keepgoing.worker.image.application.port.in.ImageProcessingCommand;
import com.keepgoing.keepgoing.worker.image.application.port.out.ImageProcessingResultPublisherPort;
import com.keepgoing.keepgoing.worker.image.application.port.out.ImageStorageException;
import com.keepgoing.keepgoing.worker.image.application.port.out.ImageStoragePort;
import com.keepgoing.keepgoing.worker.image.domain.ImageValidationFailureReason;
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

	@Mock
	ImageStoragePort imageStoragePort;

	ImageProcessingService imageProcessingService;

	@BeforeEach
	void setup() {
		imageProcessingService = new ImageProcessingService(
				Clock.fixed(PROCESSED_AT, ZoneOffset.UTC),
				resultPublisher,
				imageStoragePort
		);
	}

	@Test
	@DisplayName("quarantine object가 요청 MIME과 일치하는 이미지이면 SCANNING만 발행하고 cleanup 하지 않는다")
	void publishesOnlyScanningWhenImageMediaTypeValidationSucceeds() {
		// given
		ImageProcessingCommand command = command("image/png");
		byte[] imageBytes = pngBytes();

		given(imageStoragePort.readQuarantineObject(command.storageKey()))
				.willReturn(imageBytes);

		// when
		imageProcessingService.process(command);

		// then
		ArgumentCaptor<ImageProcessingResultEvent> eventCaptor
				= ArgumentCaptor.forClass(ImageProcessingResultEvent.class);

		then(imageStoragePort).should()
				.readQuarantineObject(command.storageKey());
		then(imageStoragePort).should(never())
				.deleteQuarantineObject(anyString());
		then(resultPublisher).should(times(1))
				.publish(eventCaptor.capture());

		ImageProcessingResultEvent event = eventCaptor.getValue();
		assertThat(event.publicId()).isEqualTo(command.publicId());
		assertThat(event.status()).isEqualTo(ImageProcessingStatus.SCANNING);
		assertThat(event.reason()).isEmpty();
		assertThat(event.processedAt()).isEqualTo(PROCESSED_AT);
	}

	@Test
	@DisplayName("요청 MIME과 실제 감지 MIME이 다르면 object를 삭제하고 REJECTED 결과를 발행한다")
	void deletesObjectAndPublishesRejectedWhenMediaTypeValidationFails() {
		// given
		ImageProcessingCommand command = command("image/jpeg");
		byte[] imageByes = pngBytes();

		given(imageStoragePort.readQuarantineObject(command.storageKey()))
				.willReturn(imageByes);

		// when
		imageProcessingService.process(command);

		// then
		ArgumentCaptor<ImageProcessingResultEvent> eventCaptor
				= ArgumentCaptor.forClass(ImageProcessingResultEvent.class);

		InOrder inOrder = inOrder(resultPublisher, imageStoragePort);
		inOrder.verify(imageStoragePort).readQuarantineObject(command.storageKey());
		inOrder.verify(resultPublisher).publish(eventCaptor.capture());
		inOrder.verify(imageStoragePort).deleteQuarantineObject(command.storageKey());
		inOrder.verify(resultPublisher).publish(eventCaptor.capture());

		assertThat(eventCaptor.getAllValues())
				.extracting(ImageProcessingResultEvent::status)
				.containsExactly(
						ImageProcessingStatus.SCANNING,
						ImageProcessingStatus.REJECTED
				);

		ImageProcessingResultEvent scanningEvent = eventCaptor.getAllValues().get(0);
		assertThat(scanningEvent.publicId()).isEqualTo(command.publicId());
		assertThat(scanningEvent.reason()).isEmpty();
		assertThat(scanningEvent.processedAt()).isEqualTo(PROCESSED_AT);

		ImageProcessingResultEvent rejectedEvent = eventCaptor.getAllValues().get(1);
		assertThat(rejectedEvent.publicId()).isEqualTo(command.publicId());
		assertThat(rejectedEvent.reason())
				.isEqualTo(ImageValidationFailureReason.CONTENT_TYPE_MISMATCH.name());
		assertThat(rejectedEvent.processedAt()).isEqualTo(PROCESSED_AT);
	}

	@Test
	@DisplayName("quarantine object 읽기에 실패하면 예외를 전파하고 cleanup과 REJECTED 발행을 하지 않는다")
	void propagatesExceptionWhenReadingQuarantineObjectFails() {
		// given
		ImageProcessingCommand command = command("image/png");
		ImageStorageException exception = new ImageStorageException("quarantine object 읽기 실패",
				new RuntimeException());

		given(imageStoragePort.readQuarantineObject(command.storageKey()))
				.willThrow(exception);

		// when & then
		assertThatThrownBy(() -> imageProcessingService.process(command))
				.isSameAs(exception);

		then(imageStoragePort).should()
				.readQuarantineObject(command.storageKey());
		then(imageStoragePort).should(never())
				.deleteQuarantineObject(command.storageKey());

		then(resultPublisher).shouldHaveNoInteractions();
	}

	private static ImageProcessingCommand command(String contentType) {
		return new ImageProcessingCommand(
				UUID.randomUUID(),
				"notes/10/generated-image-key",
				contentType,
				1024L,
				REQUESTED_AT
		);
	}

	private static byte[] pngBytes() {
		return new byte[]{
				(byte) 0x89, 0x50, 0x4E, 0x47,
				0x0D, 0x0A, 0x1A, 0x0A,
				0x00, 0x00
		};
	}
}