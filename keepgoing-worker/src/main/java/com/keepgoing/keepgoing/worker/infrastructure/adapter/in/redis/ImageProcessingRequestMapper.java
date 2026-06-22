package com.keepgoing.keepgoing.worker.infrastructure.adapter.in.redis;

import com.keepgoing.keepgoing.common.image.event.ImageProcessingRequestedEvent;
import com.keepgoing.keepgoing.worker.application.port.in.ImageProcessingCommand;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class ImageProcessingRequestMapper {

	static ImageProcessingCommand toCommand(ImageProcessingRequestedEvent event) {
		return new ImageProcessingCommand(
				event.publicId(),
				event.storageKey(),
				event.contentType(),
				event.fileSize(),
				event.requestedAt()
		);
	}
}
