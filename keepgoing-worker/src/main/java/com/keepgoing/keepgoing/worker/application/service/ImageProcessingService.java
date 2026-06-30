package com.keepgoing.keepgoing.worker.application.service;

import com.keepgoing.keepgoing.common.image.event.ImageProcessingResultEvent;
import com.keepgoing.keepgoing.worker.application.dto.PreValidatedImage;
import com.keepgoing.keepgoing.worker.application.dto.SanitizedImage;
import com.keepgoing.keepgoing.worker.application.port.in.ImageProcessingCommand;
import com.keepgoing.keepgoing.worker.application.port.in.ImageProcessingUseCase;
import com.keepgoing.keepgoing.worker.application.port.out.ImageMediaTypeDetectorPort;
import com.keepgoing.keepgoing.worker.application.port.out.ImageProcessingResultPublisherPort;
import com.keepgoing.keepgoing.worker.application.port.out.ImageSanitizerPort;
import com.keepgoing.keepgoing.worker.application.port.out.ImageStoragePort;
import com.keepgoing.keepgoing.worker.application.port.out.QuarantineObjectNotFoundException;
import com.keepgoing.keepgoing.worker.domain.ImageMediaTypeValidator;
import com.keepgoing.keepgoing.worker.domain.ImageValidationFailureReason;
import com.keepgoing.keepgoing.worker.domain.ImageValidationResult;
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
	private final ImageMediaTypeDetectorPort imageMediaTypeDetector;

	@Override
	public void process(ImageProcessingCommand command) {
		UUID publicId = command.publicId();
		String storageKey = command.storageKey();
		String requestedContentType = command.contentType();

		byte[] imageBytes;
		try {
			imageBytes = imageStoragePort.readQuarantineObject(storageKey);
		} catch (QuarantineObjectNotFoundException e) {
			log.warn("이미 삭제된 파일, 처리 생략: publicId={}, storageKey={}", publicId, storageKey);
			return;
		}

		publishScanningEvent(command);

		String detectedContentType = imageMediaTypeDetector.detect(imageBytes);
		ImageValidationResult validationResult
				= ImageMediaTypeValidator.validate(requestedContentType, detectedContentType);

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
