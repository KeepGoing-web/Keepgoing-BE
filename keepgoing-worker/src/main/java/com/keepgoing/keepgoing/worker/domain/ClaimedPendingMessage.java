package com.keepgoing.keepgoing.worker.domain;

import com.keepgoing.keepgoing.worker.application.port.in.ImageProcessingCommand;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ClaimedPendingMessage {

	private String messageId;
	private UUID publicId;
	private String storageKey;
	private String contentType;
	private long fileSize;
	private Instant requestedAt;
	private int retryCount;

	public void incrementRetryCount() {
		++this.retryCount;
	}

	public boolean isRetryExhausted(int maxRetries) {
		return retryCount >= maxRetries;
	}

	public ImageProcessingCommand toCommand() {
		return new ImageProcessingCommand(
				publicId, storageKey, contentType, fileSize, requestedAt, retryCount
		);
	}
}
