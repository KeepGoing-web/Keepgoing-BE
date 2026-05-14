package com.keepgoing.keepgoing.global.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage")
public record StorageProperties(
		String endpoint,
		String accessKey,
		String secretKey,
		String region,
		BucketNames bucketNames
) {
	public record BucketNames(
			String quarantine,
			String secure
	) {
	}
}