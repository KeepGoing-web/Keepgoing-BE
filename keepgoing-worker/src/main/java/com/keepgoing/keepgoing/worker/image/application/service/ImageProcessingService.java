package com.keepgoing.keepgoing.worker.image.application.service;

import com.keepgoing.keepgoing.common.image.event.ImageProcessingResultEvent;
import com.keepgoing.keepgoing.worker.image.application.dto.PreValidatedImage;
import com.keepgoing.keepgoing.worker.image.application.dto.SanitizedImage;
import com.keepgoing.keepgoing.worker.image.application.port.in.ImageProcessingCommand;
import com.keepgoing.keepgoing.worker.image.application.port.in.ImageProcessingUseCase;
import com.keepgoing.keepgoing.worker.image.application.port.out.ImageProcessingResultPublisherPort;
import com.keepgoing.keepgoing.worker.image.application.port.out.ImageSanitizerPort;
import com.keepgoing.keepgoing.worker.image.application.port.out.ImageStoragePort;
import com.keepgoing.keepgoing.worker.image.domain.ImageMediaTypeValidator;
import com.keepgoing.keepgoing.worker.image.domain.ImageValidationFailureReason;
import com.keepgoing.keepgoing.worker.image.domain.ImageValidationResult;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageProcessingService implements ImageProcessingUseCase {

	private final Clock clock;
	private final ImageProcessingResultPublisherPort resultPublisher;
	private final ImageStoragePort imageStoragePort;
	private final ImageSanitizerPort imageSanitizerPort;

	@Override
	public void process(ImageProcessingCommand command) {
		UUID publicId = command.publicId();
		String storageKey = command.storageKey();
		String requestedContentType = command.contentType();

		byte[] imageBytes = imageStoragePort.readQuarantineObject(storageKey);

		publishScanningEvent(command);

		ImageValidationResult validationResult
				= ImageMediaTypeValidator.validate(imageBytes, requestedContentType);

		if (!validationResult.valid()) {
			imageStoragePort.deleteQuarantineObject(storageKey);
			publishRejectedEvent(command, validationResult.reason().name());
			return;
		}

		var preValidatedImage = new PreValidatedImage(
				publicId,
				storageKey,
				requestedContentType,
				validationResult.detectedContentType(),
				command.fileSize(),
				command.requestedAt()
		);

		SanitizedImage sanitizedImage;
		try {
			sanitizedImage = imageSanitizerPort.sanitize(preValidatedImage, imageBytes);
		} catch (RuntimeException e) {
			log.warn("이미지 정제 실패: publicId={}, storageKey={}", publicId, storageKey, e);
			imageStoragePort.deleteQuarantineObject(storageKey);
			publishRejectedEvent(command, ImageValidationFailureReason.SANITIZATION_FAILED.name());
			return;
		}

		imageStoragePort.putSecureObject(
				preValidatedImage.storageKey(),
				sanitizedImage.bytes(),
				sanitizedImage.contentType()
		);

		imageStoragePort.deleteQuarantineObject(storageKey);
		publishSafeEvent(preValidatedImage, sanitizedImage);
	}

	private void publishScanningEvent(ImageProcessingCommand command) {
		resultPublisher.publish(ImageProcessingResultEvent.scanning(
				command.publicId(),
				Instant.now(clock)
		));
	}

	private void publishRejectedEvent(ImageProcessingCommand command, String reason) {
		resultPublisher.publish(ImageProcessingResultEvent.rejected(
				command.publicId(),
				reason,
				Instant.now(clock)
		));
	}

	private void publishSafeEvent(PreValidatedImage image, SanitizedImage sanitizedImage) {
		resultPublisher.publish(ImageProcessingResultEvent.safe(
				image.publicId(),
				Instant.now(clock),
				image.storageKey(),
				sanitizedImage.contentType(),
				sanitizedImage.fileSize()
		));
	}
}
