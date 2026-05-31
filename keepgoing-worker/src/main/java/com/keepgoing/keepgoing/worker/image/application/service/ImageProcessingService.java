package com.keepgoing.keepgoing.worker.image.application.service;

import com.keepgoing.keepgoing.common.image.domain.ImageProcessingStatus;
import com.keepgoing.keepgoing.common.image.event.ImageProcessingResultEvent;
import com.keepgoing.keepgoing.worker.image.application.port.in.ImageProcessingCommand;
import com.keepgoing.keepgoing.worker.image.application.port.in.ImageProcessingUseCase;
import com.keepgoing.keepgoing.worker.image.application.port.out.ImageProcessingResultPublisherPort;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ImageProcessingService implements ImageProcessingUseCase {

	private final Clock clock;
	private final ImageProcessingResultPublisherPort resultPublisher;

	@Override
	public void process(ImageProcessingCommand command) {
		resultPublisher.publish(new ImageProcessingResultEvent(
				command.publicId(),
				ImageProcessingStatus.SCANNING,
				"",
				Instant.now(clock)
		));

		// TODO: MinIO download -> validation/sanitization -> secure upload

		resultPublisher.publish(new ImageProcessingResultEvent(
				command.publicId(),
				ImageProcessingStatus.SAFE,
				"",
				Instant.now(clock)
		));
	}
}
