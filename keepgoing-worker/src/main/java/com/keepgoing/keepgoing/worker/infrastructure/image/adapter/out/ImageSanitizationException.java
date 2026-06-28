package com.keepgoing.keepgoing.worker.infrastructure.image.adapter.out;

public class ImageSanitizationException extends RuntimeException{
	public ImageSanitizationException(String message) {
		super(message);
	}

	public ImageSanitizationException(String message, Throwable cause) {
		super(message, cause);
	}
}
