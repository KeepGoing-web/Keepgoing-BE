package com.keepgoing.keepgoing.worker.infrastructure.config.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;

import com.keepgoing.keepgoing.common.image.event.ImageProcessingRequestedEvent;
import com.keepgoing.keepgoing.worker.application.port.in.ImageProcessingCommand;
import com.keepgoing.keepgoing.worker.application.port.in.ImageProcessingUseCase;
import com.keepgoing.keepgoing.worker.infrastructure.adapter.in.schedule.PendingMessageRecoveryScheduler;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(properties = {
		"image-processing.streams.enabled=true"
})
@Testcontainers
class WorkerRedisStreamIntegrationTest {

	@Container
	@SuppressWarnings("resource")
	static final GenericContainer<?> redis =
			new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
					.withExposedPorts(6379);

	@DynamicPropertySource
	static void redisProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.redis.host", redis::getHost);
		registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
	}

	@Autowired
	StringRedisTemplate redisTemplate;

	@Autowired
	WorkerRedisStreamProperties properties;

	@MockitoBean
	ImageProcessingUseCase imageProcessingUseCase;

	@MockitoBean
	PendingMessageRecoveryScheduler recoveryScheduler;

	@AfterEach
	void tearDown() {
		redisTemplate.delete(properties.request());
		redisTemplate.delete(properties.result());
	}

	@Test
	@DisplayName("request stream 메시지를 소비해 use case에 전달하고 ack한다.")
	void consumesRequestEventPublishesResultsAndAcknowledges() {
		// given
		UUID publicId = UUID.randomUUID();
		ImageProcessingRequestedEvent event = new ImageProcessingRequestedEvent(
				publicId,
				"notes/10/generated-image-key",
				"image/png",
				1024L,
				Instant.parse("2026-05-15T00:00:00Z"),
				0
		);

		// when
		redisTemplate.opsForStream()
				.add(properties.request(), event.toMap());

		// then
		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			then(imageProcessingUseCase).should()
					.process(any(ImageProcessingCommand.class));
		});

		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			var pending = redisTemplate.opsForStream()
					.pending(properties.request(), properties.requestGroup());

			assertThat(pending).isNotNull();
			assertThat(pending.getTotalPendingMessages()).isZero();
		});
	}
}
