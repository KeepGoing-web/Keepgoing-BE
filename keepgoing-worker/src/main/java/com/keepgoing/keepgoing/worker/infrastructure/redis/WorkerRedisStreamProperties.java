package com.keepgoing.keepgoing.worker.infrastructure.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "image-processing.streams")
public record WorkerRedisStreamProperties(
		boolean enabled,
		String request,
		String result,
		String requestGroup,
		String requestConsumer
) {
}
