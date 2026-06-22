package com.keepgoing.keepgoing.worker.infrastructure.adapter.in.redis;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

import com.keepgoing.keepgoing.common.image.event.ImageProcessingRequestedEvent;
import com.keepgoing.keepgoing.worker.application.port.in.ImageProcessingCommand;
import com.keepgoing.keepgoing.worker.application.port.in.ImageProcessingUseCase;
import com.keepgoing.keepgoing.worker.infrastructure.config.redis.WorkerRedisStreamProperties;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class RedisImageProcessingRequestListenerTest {

	private static final String REQUEST_STREAM = "note-image-processing-requests";
	private static final String RESULT_STREAM = "note-image-processing-results";
	private static final String REQUEST_GROUP = "keepgoing-worker";
	private static final String REQUEST_CONSUMER = "worker-1";
	private static final RecordId RECORD_ID = RecordId.of("1715750400000-0");
	private static final Instant REQUESTED_AT = Instant.parse("2026-05-15T00:00:00Z");
	private static final Instant PROCESSED_AT = Instant.parse("2026-05-15T00:01:00Z");

	@Mock
	StringRedisTemplate redisTemplate;

	@Mock
	StreamOperations<String, Object, Object> streamOperations;

	@Mock
	ImageProcessingUseCase useCase;

	WorkerRedisStreamProperties properties;
	RedisImageProcessingRequestListener listener;

	@BeforeEach
	void setUp() {
		properties = new WorkerRedisStreamProperties(
				true,
				REQUEST_STREAM,
				RESULT_STREAM,
				REQUEST_GROUP,
				REQUEST_CONSUMER
		);
		listener = new RedisImageProcessingRequestListener(
				redisTemplate,
				properties,
				useCase
		);
	}

	@Test
	@DisplayName("이미지 처리 요청 메시지를 use case에 전달한 뒤 ack한다")
	void publishesScanningAndSafeResultsThenAcknowledgesMessage() {
		// given
		ImageProcessingRequestedEvent requestedEvent = requestedEvent();
		MapRecord<String, String, String> message = message(requestedEvent);
		given(redisTemplate.opsForStream()).willReturn(streamOperations);

		// when
		listener.handle(message);

		// then
		InOrder inOrder = inOrder(useCase, redisTemplate, streamOperations);
		inOrder.verify(useCase).process(any(ImageProcessingCommand.class));
		inOrder.verify(redisTemplate).opsForStream();
		inOrder.verify(streamOperations).acknowledge(REQUEST_STREAM, REQUEST_GROUP, RECORD_ID);
	}

	@Test
	@DisplayName("use case 처리 중 예외가 발생하면 request stream 메시지를 ack하지 않는다.")
	void doesNotAcknowledgeMessageWhenPublishingResultFails() {
		// given
		ImageProcessingRequestedEvent requestedEvent = requestedEvent();
		MapRecord<String, String, String> message = message(requestedEvent);
		RuntimeException exception = new RuntimeException("publish failed");

		willThrow(exception)
				.given(useCase)
				.process(any(ImageProcessingCommand.class));

		// when & then
		assertThatThrownBy(() -> listener.handle(message))
				.isSameAs(exception);

		then(useCase).should().process(any(ImageProcessingCommand.class));
		then(redisTemplate).should(never()).opsForStream();
		then(streamOperations).shouldHaveNoInteractions();
	}

	private static ImageProcessingRequestedEvent requestedEvent() {
		return new ImageProcessingRequestedEvent(
				UUID.randomUUID(),
				"notes/10/generated-image-key",
				"image/png",
				1024L,
				REQUESTED_AT
		);
	}

	private static MapRecord<String, String, String> message(ImageProcessingRequestedEvent event) {
		return MapRecord.create(REQUEST_STREAM, event.toMap()).withId(RECORD_ID);
	}
}
