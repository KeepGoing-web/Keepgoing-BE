package com.keepgoing.keepgoing.worker.image.application.service;

import com.keepgoing.keepgoing.common.image.domain.ImageProcessingStatus;
import com.keepgoing.keepgoing.common.image.event.ImageProcessingResultEvent;
import com.keepgoing.keepgoing.worker.image.application.dto.PreValidatedImage;
import com.keepgoing.keepgoing.worker.image.application.dto.SanitizedImage;
import com.keepgoing.keepgoing.worker.image.application.port.in.ImageProcessingCommand;
import com.keepgoing.keepgoing.worker.image.application.port.in.ImageProcessingUseCase;
import com.keepgoing.keepgoing.worker.image.application.port.out.ImageProcessingResultPublisherPort;
import com.keepgoing.keepgoing.worker.image.application.port.out.ImageSanitizerPort;
import com.keepgoing.keepgoing.worker.image.application.port.out.ImageStoragePort;
import com.keepgoing.keepgoing.worker.image.domain.ImageMediaTypeValidator;
import com.keepgoing.keepgoing.worker.image.domain.ImageValidationResult;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ImageProcessingService implements ImageProcessingUseCase {

	private final Clock clock;
	private final ImageProcessingResultPublisherPort resultPublisher;
	private final ImageStoragePort imageStoragePort;
	private final ImageSanitizerPort imageSanitizerPort;

	@Override
	public void process(ImageProcessingCommand command) {
		byte[] imageBytes = imageStoragePort.readQuarantineObject(command.storageKey());

		publishResultEvent(command, ImageProcessingStatus.SCANNING, "");

		ImageValidationResult validationResult
				= ImageMediaTypeValidator.validate(imageBytes, command.contentType());

		if (!validationResult.valid()) {
			imageStoragePort.deleteQuarantineObject(command.storageKey());
			publishResultEvent(command, ImageProcessingStatus.REJECTED, validationResult.reason().name());
			return;
		}

		var preValidatedImage = new PreValidatedImage(
				command.publicId(),
				command.storageKey(),
				command.contentType(),
				validationResult.detectedContentType(),
				command.fileSize(),
				command.requestedAt()
		);

		SanitizedImage sanitizedImage = imageSanitizerPort.sanitize(preValidatedImage, imageBytes);

		imageStoragePort.putSecureObject(
				preValidatedImage.storageKey(),
				sanitizedImage.bytes(),
				sanitizedImage.contentType()
		);

		imageStoragePort.deleteQuarantineObject(command.storageKey());
	}

	private void publishResultEvent(ImageProcessingCommand command, ImageProcessingStatus status, String reason) {
		resultPublisher.publish(new ImageProcessingResultEvent(
				command.publicId(),
				status,
				reason,
				Instant.now(clock)
		));
	}
}
