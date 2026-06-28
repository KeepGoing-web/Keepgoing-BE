package com.keepgoing.keepgoing.worker.image.application.port.out;

import com.keepgoing.keepgoing.worker.image.application.dto.PreValidatedImage;
import com.keepgoing.keepgoing.worker.image.application.dto.SanitizedImage;

public interface ImageSanitizerPort {

	SanitizedImage sanitize(PreValidatedImage image, byte[] originalBytes);
}
