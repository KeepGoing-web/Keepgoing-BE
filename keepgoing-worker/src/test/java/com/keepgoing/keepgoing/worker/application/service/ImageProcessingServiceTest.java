package com.keepgoing.keepgoing.worker.application.service;

import static com.keepgoing.keepgoing.worker.support.ImageFixture.pngBytes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

import com.keepgoing.keepgoing.common.image.domain.ImageProcessingStatus;
import com.keepgoing.keepgoing.common.image.event.ImageProcessingResultEvent;
import com.keepgoing.keepgoing.worker.application.dto.PreValidatedImage;
import com.keepgoing.keepgoing.worker.application.dto.SanitizedImage;
import com.keepgoing.keepgoing.worker.application.port.in.ImageProcessingCommand;
import com.keepgoing.keepgoing.worker.application.port.out.ImageMediaTypeDetectorPort;
import com.keepgoing.keepgoing.worker.application.port.out.ImageProcessingResultPublisherPort;
import com.keepgoing.keepgoing.worker.application.port.out.ImageSanitizerPort;
import com.keepgoing.keepgoing.worker.application.port.out.ImageStorageException;
import com.keepgoing.keepgoing.worker.application.port.out.ImageStoragePort;
import com.keepgoing.keepgoing.worker.domain.ImageValidationFailureReason;
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
	public static final String IMAGE_PNG = "image/png";
	public static final String IMAGE_JPEG = "image/jpeg";

	@Mock
	ImageProcessingResultPublisherPort resultPublisher;

	@Mock
	ImageStoragePort imageStorage;

	@Mock
	ImageSanitizerPort imageSanitizer;

	@Mock
	ImageMediaTypeDetectorPort imageMediaTypeDetector;

	ImageProcessingService imageProcessingService;

	@BeforeEach
	void setup() {
		imageProcessingService = new ImageProcessingService(
				Clock.fixed(PROCESSED_AT, ZoneOffset.UTC),
				resultPublisher,
				imageStorage,
				imageSanitizer,
				imageMediaTypeDetector
		);
	}

	@Test
	@DisplayName("검증된 이미지는 정제 후 secure bucket에 저장하고 quarantine object를 삭제한다")
	void sanitizesAndStoresSecureObjectWhenImageMediaTypeValidationSucceeds() {
		// given
		ImageProcessingCommand command = command(IMAGE_PNG);
		byte[] imageBytes = pngBytes();
		byte[] sanitizedBytes = new byte[]{1, 2, 3};
		SanitizedImage sanitizedImage = new SanitizedImage(
				sanitizedBytes,
				IMAGE_PNG,
				sanitizedBytes.length
		);

		given(imageStorage.readQuarantineObject(command.storageKey()))
				.willReturn(imageBytes);
		given(imageMediaTypeDetector.detect(imageBytes))
				.willReturn(IMAGE_PNG);
		given(imageSanitizer.sanitize(any(PreValidatedImage.class), eq(imageBytes)))
				.willReturn(sanitizedImage);

		// when
		imageProcessingService.process(command);

		// then
		ArgumentCaptor<ImageProcessingResultEvent> eventCaptor
				= ArgumentCaptor.forClass(ImageProcessingResultEvent.class);
		ArgumentCaptor<PreValidatedImage> preValidatedImageCaptor
				= ArgumentCaptor.forClass(PreValidatedImage.class);

		InOrder inOrder = inOrder(resultPublisher, imageStorage, imageSanitizer, imageMediaTypeDetector);
		inOrder.verify(imageStorage).readQuarantineObject(command.storageKey());
		inOrder.verify(resultPublisher).publish(eventCaptor.capture());
		inOrder.verify(imageMediaTypeDetector).detect(imageBytes);
		inOrder.verify(imageSanitizer).sanitize(preValidatedImageCaptor.capture(), eq(imageBytes));
		inOrder.verify(imageStorage).putSecureObject(
				command.storageKey(),
				sanitizedBytes,
				sanitizedImage.contentType()
		);
		inOrder.verify(imageStorage).deleteQuarantineObject(command.storageKey());
		inOrder.verify(resultPublisher).publish(eventCaptor.capture());

		assertThat(eventCaptor.getAllValues())
				.extracting(ImageProcessingResultEvent::status)
				.containsExactly(
						ImageProcessingStatus.SCANNING,
						ImageProcessingStatus.SAFE
				);

		assertThat(preValidatedImageCaptor.getValue()).isEqualTo(new PreValidatedImage(
				command.publicId(),
				command.storageKey(),
				command.contentType(),
				IMAGE_PNG,
				command.fileSize(),
				command.requestedAt()
		));

		ImageProcessingResultEvent safeEvent = eventCaptor.getAllValues().get(1);
		assertThat(safeEvent.secureStorageKey()).isEqualTo(command.storageKey());
		assertThat(safeEvent.contentType()).isEqualTo(sanitizedImage.contentType());
		assertThat(safeEvent.fileSize()).isEqualTo(sanitizedImage.fileSize());
	}

	@Test
	@DisplayName("요청 MIME과 실제 감지 MIME이 다르면 object를 삭제하고 REJECTED 결과를 발행한다")
	void deletesObjectAndPublishesRejectedWhenMediaTypeValidationFails() {
		// given
		ImageProcessingCommand command = command(IMAGE_JPEG);
		byte[] imageByes = pngBytes();

		given(imageStorage.readQuarantineObject(command.storageKey()))
				.willReturn(imageByes);
		given(imageMediaTypeDetector.detect(imageByes))
				.willReturn(IMAGE_PNG);

		// when
		imageProcessingService.process(command);

		// then
		ArgumentCaptor<ImageProcessingResultEvent> eventCaptor
				= ArgumentCaptor.forClass(ImageProcessingResultEvent.class);

		InOrder inOrder = inOrder(resultPublisher, imageStorage, imageMediaTypeDetector);
		inOrder.verify(imageStorage).readQuarantineObject(command.storageKey());
		inOrder.verify(resultPublisher).publish(eventCaptor.capture());
		inOrder.verify(imageMediaTypeDetector).detect(imageByes);
		inOrder.verify(imageStorage).deleteQuarantineObject(command.storageKey());
		inOrder.verify(resultPublisher).publish(eventCaptor.capture());

		assertThat(eventCaptor.getAllValues())
				.extracting(ImageProcessingResultEvent::status)
				.containsExactly(
						ImageProcessingStatus.SCANNING,
						ImageProcessingStatus.REJECTED
				);

		ImageProcessingResultEvent scanningEvent = eventCaptor.getAllValues().getFirst();
		assertThat(scanningEvent.publicId()).isEqualTo(command.publicId());
		assertThat(scanningEvent.reason()).isEmpty();
		assertThat(scanningEvent.processedAt()).isEqualTo(PROCESSED_AT);

		ImageProcessingResultEvent rejectedEvent = eventCaptor.getAllValues().get(1);
		assertThat(rejectedEvent.publicId()).isEqualTo(command.publicId());
		assertThat(rejectedEvent.reason())
				.isEqualTo(ImageValidationFailureReason.CONTENT_TYPE_MISMATCH.name());
		assertThat(rejectedEvent.processedAt()).isEqualTo(PROCESSED_AT);

		then(imageSanitizer).shouldHaveNoInteractions();
		then(imageStorage).should(never())
				.putSecureObject(anyString(), any(), anyString());
	}

	@Test
	@DisplayName("quarantine object 읽기에 실패하면 예외를 전파하고 cleanup과 REJECTED 발행을 하지 않는다")
	void propagatesExceptionWhenReadingQuarantineObjectFails() {
		// given
		ImageProcessingCommand command = command(IMAGE_PNG);
		ImageStorageException exception = new ImageStorageException("quarantine object 읽기 실패",
				new RuntimeException());

		given(imageStorage.readQuarantineObject(command.storageKey()))
				.willThrow(exception);

		// when & then
		assertThatThrownBy(() -> imageProcessingService.process(command))
				.isSameAs(exception);

		then(imageStorage).should()
				.readQuarantineObject(command.storageKey());
		then(imageStorage).should(never())
				.deleteQuarantineObject(command.storageKey());

		then(resultPublisher).shouldHaveNoInteractions();
		then(imageSanitizer).shouldHaveNoInteractions();
		then(imageStorage).should(never())
				.putSecureObject(anyString(), any(), anyString());
	}

	@Test
	@DisplayName("이미지 정제에 실패하면 object를 삭제하고 REJECTED 결과를 발행한다")
	void deletesObjectAndPublishedRejectedWhenImageSanitizationFails() {
		// given
		ImageProcessingCommand command = command(IMAGE_PNG);
		byte[] imageBytes = pngBytes();

		given(imageStorage.readQuarantineObject(command.storageKey()))
				.willReturn(imageBytes);
		given(imageMediaTypeDetector.detect(imageBytes))
				.willReturn(IMAGE_PNG);
		given(imageSanitizer.sanitize(any(PreValidatedImage.class), eq(imageBytes)))
				.willThrow(new RuntimeException("sanitize failed"));

		// when
		imageProcessingService.process(command);

		// then
		ArgumentCaptor<ImageProcessingResultEvent> eventCaptor =
				ArgumentCaptor.forClass(ImageProcessingResultEvent.class);

		InOrder inOrder = inOrder(resultPublisher, imageStorage, imageSanitizer, imageMediaTypeDetector);
		inOrder.verify(imageStorage).readQuarantineObject(command.storageKey());
		inOrder.verify(resultPublisher).publish(eventCaptor.capture());
		inOrder.verify(imageMediaTypeDetector).detect(imageBytes);
		inOrder.verify(imageSanitizer).sanitize(any(PreValidatedImage.class), eq(imageBytes));
		inOrder.verify(imageStorage).deleteQuarantineObject(command.storageKey());
		inOrder.verify(resultPublisher).publish(eventCaptor.capture());

		assertThat(eventCaptor.getAllValues())
				.extracting(ImageProcessingResultEvent::status)
				.containsExactly(
						ImageProcessingStatus.SCANNING,
						ImageProcessingStatus.REJECTED
				);

		ImageProcessingResultEvent rejectedEvent = eventCaptor.getAllValues().get(1);
		assertThat(rejectedEvent.reason())
				.isEqualTo(ImageValidationFailureReason.SANITIZATION_FAILED.name());

		then(imageStorage).should(never())
				.putSecureObject(anyString(), any(), anyString());

	}

	private static ImageProcessingCommand command(String contentType) {
		return new ImageProcessingCommand(
				UUID.randomUUID(),
				"notes/10/generated-image-key",
				contentType,
				1024L,
				REQUESTED_AT,
				0
		);
	}
}
