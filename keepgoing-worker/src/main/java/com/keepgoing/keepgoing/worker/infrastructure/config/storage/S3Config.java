package com.keepgoing.keepgoing.worker.infrastructure.config.storage;

import java.net.URI;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(StorageProperties.class)
public class S3Config {

	private final StorageProperties properties;

	@Bean
	public S3Client s3Client() {
		return S3Client.builder()
				.region(Region.of(properties.region()))
				.endpointOverride(URI.create(properties.endpoint()))
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())
				))
				.forcePathStyle(true)
				.overrideConfiguration(ClientOverrideConfiguration.builder()
						.apiCallTimeout(Duration.ofSeconds(60))
						.apiCallAttemptTimeout(Duration.ofSeconds(30))
						.retryPolicy(RetryMode.STANDARD)
						.build())
				.build();
	}
}
