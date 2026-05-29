package com.keepgoing.keepgoing.global.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "image-processing.streams")
public record RedisStreamProperties(
		boolean enabled,
		String request,
		String result,
		String resultGroup,
		String resultConsumer
) {
}
