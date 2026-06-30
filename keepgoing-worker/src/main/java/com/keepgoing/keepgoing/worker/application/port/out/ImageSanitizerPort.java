package com.keepgoing.keepgoing.worker.application.port.out;

import com.keepgoing.keepgoing.worker.application.dto.PreValidatedImage;
import com.keepgoing.keepgoing.worker.application.dto.SanitizedImage;

public interface ImageSanitizerPort {

	SanitizedImage sanitize(PreValidatedImage image, byte[] originalBytes);
}
