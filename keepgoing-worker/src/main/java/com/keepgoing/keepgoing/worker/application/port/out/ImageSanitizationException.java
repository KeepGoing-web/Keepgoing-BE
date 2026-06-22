package com.keepgoing.keepgoing.worker.application.port.out;

public class ImageSanitizationException extends RuntimeException{
	public ImageSanitizationException(String message) {
		super(message);
	}

	public ImageSanitizationException(String message, Throwable cause) {
		super(message, cause);
	}
}
