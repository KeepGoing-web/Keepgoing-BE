package com.keepgoing.keepgoing.worker.image.application.port.out;

import com.keepgoing.keepgoing.common.image.event.ImageProcessingResultEvent;

public interface ImageProcessingResultPublisherPort {
	void publish(ImageProcessingResultEvent event);
}
