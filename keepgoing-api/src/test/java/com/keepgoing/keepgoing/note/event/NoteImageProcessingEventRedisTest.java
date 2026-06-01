package com.keepgoing.keepgoing.note.event;

import com.keepgoing.keepgoing.support.PostgreSqlTestContainerSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.BDDMockito.then;

import com.keepgoing.keepgoing.common.image.domain.ImageProcessingStatus;
import com.keepgoing.keepgoing.common.image.event.ImageProcessingRequestedEvent;
import com.keepgoing.keepgoing.common.image.event.ImageProcessingResultEvent;
import com.keepgoing.keepgoing.global.redis.RedisStreamProperties;
import com.keepgoing.keepgoing.note.service.NoteImageService;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.PendingMessagesSummary;
import org.springframework.data.redis.connection.stream.RecordId;
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
class NoteImageProcessingEventRedisTest extends PostgreSqlTestContainerSupport {

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
	NoteImageProcessingRequestPublisher requestPublisher;

	@Autowired
	StringRedisTemplate redisTemplate;

	@Autowired
	RedisStreamProperties properties;

	@MockitoBean
	NoteImageService noteImageService;

	@AfterEach
	void tearDown() {
		redisTemplate.delete(properties.request());
	}

	@Test
	@DisplayName("이미지 처리 요청 이벤트를 request stream에 발행한다.")
	void publishesProcessingRequestEventToRequestStream() {
		// given
		ImageProcessingRequestedEvent event = new ImageProcessingRequestedEvent(
				UUID.randomUUID(),
				"notes/10/generated-image-key",
				"image/png",
				1024L,
				Instant.parse("2026-05-15T00:00:00Z")
		);

		// when
		RecordId recordId = requestPublisher.publish(event);

		// then
		var records = redisTemplate.opsForStream()
				.range(properties.request(), Range.unbounded());

		assertThat(recordId).isNotNull();
		assertThat(records).hasSize(1);
		assertThat(records.getFirst().getValue())
				.containsAllEntriesOf(event.toMap());
	}

	@Test
	@DisplayName("result stream 메시지를 소비해 서비스에 전달하고 ack한다.")
	void consumesProcessingResultEventAndAcknowledges() {
		// given
		var event = new ImageProcessingResultEvent(
				UUID.randomUUID(),
				ImageProcessingStatus.SAFE,
				"",
				Instant.parse("2026-05-15T00:00:00Z")
		);

		// when
		redisTemplate.opsForStream()
				.add(properties.result(), event.toMap());

		// then
		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			ArgumentCaptor<ImageProcessingResultEvent> captor = ArgumentCaptor.forClass(
					ImageProcessingResultEvent.class);

			then(noteImageService).should().applyProcessingResult(captor.capture());

			ImageProcessingResultEvent appliedEvent = captor.getValue();
			assertThat(appliedEvent.publicId()).isEqualTo(event.publicId());
			assertThat(appliedEvent.status()).isEqualTo(event.status());
			assertThat(appliedEvent.reason()).isEqualTo(event.reason());
			assertThat(appliedEvent.processedAt()).isEqualTo(event.processedAt());
		});

		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			PendingMessagesSummary pending = redisTemplate.opsForStream()
					.pending(properties.result(), properties.resultGroup());

			assertThat(pending).isNotNull();
			assertThat(pending.getTotalPendingMessages()).isZero();
		});
	}
}
