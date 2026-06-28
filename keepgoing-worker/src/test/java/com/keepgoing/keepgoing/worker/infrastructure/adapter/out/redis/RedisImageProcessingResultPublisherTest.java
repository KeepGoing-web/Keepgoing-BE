package com.keepgoing.keepgoing.worker.infrastructure.adapter.out.redis;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.keepgoing.keepgoing.common.image.event.ImageProcessingResultEvent;
import com.keepgoing.keepgoing.worker.application.port.out.ImageProcessingResultPublisherPort;
import com.keepgoing.keepgoing.worker.infrastructure.config.redis.WorkerRedisStreamProperties;
import com.keepgoing.keepgoing.worker.support.WorkerRedisStreamPropertiesFixture;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class RedisImageProcessingResultPublisherTest {

	private static final String REQUEST_STREAM = "note-image-processing-requests";
	private static final String RESULT_STREAM = "note-image-processing-results";
	private static final String REQUEST_GROUP = "keepgoing-worker";
	private static final String REQUEST_CONSUMER = "worker-1";
	private static final Instant PROCESSED_AT = Instant.parse("2026-05-15T00:01:00Z");
	public static final String IMAGE_PROCESSING_DLQ = "image-processing-dlq";

	@Mock
	StringRedisTemplate redisTemplate;

	@Mock
	StreamOperations<String, Object, Object> streamOperations;

	WorkerRedisStreamProperties properties;
	ImageProcessingResultPublisherPort publisher;

	@BeforeEach
	void setUp() {
		properties = WorkerRedisStreamPropertiesFixture.createDefault();
		publisher = new RedisImageProcessingResultPublisher(redisTemplate, properties);
	}

	@Test
	@DisplayName("이미지 처리 결과 이벤트를 result stream에 발행한다")
	void publishesResultEventToResultStream() {
		// given
		ImageProcessingResultEvent event = ImageProcessingResultEvent.safe(
				UUID.randomUUID(),
				PROCESSED_AT,
				"notes/10/generated-image-key",
				"image/png",
				1024L
		);
		given(redisTemplate.opsForStream()).willReturn(streamOperations);

		// when
		publisher.publish(event);

		// then
		then(streamOperations).should().add(properties.result(), event.toMap());
	}
}
