package com.keepgoing.keepgoing.worker.image.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.keepgoing.keepgoing.common.image.domain.ImageProcessingStatus;
import com.keepgoing.keepgoing.common.image.event.ImageProcessingRequestedEvent;
import com.keepgoing.keepgoing.common.image.event.ImageProcessingResultEvent;
import com.keepgoing.keepgoing.worker.global.redis.WorkerRedisStreamProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class ImageProcessingRequestListenerTest {

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
	ImageProcessingResultPublisher resultPublisher;

	WorkerRedisStreamProperties properties;
	ImageProcessingRequestListener listener;

	@BeforeEach
	void setUp() {
		properties = new WorkerRedisStreamProperties(
				true,
				REQUEST_STREAM,
				RESULT_STREAM,
				REQUEST_GROUP,
				REQUEST_CONSUMER
		);
		listener = new ImageProcessingRequestListener(
				Clock.fixed(PROCESSED_AT, ZoneOffset.UTC),
				redisTemplate,
				properties,
				resultPublisher
		);
	}

	@Test
	@DisplayName("이미지 처리 요청을 받으면 SCANNING과 SAFE 결과를 순서대로 발행한 뒤 request stream 메시지를 ack한다")
	void publishesScanningAndSafeResultsThenAcknowledgesMessage() {
		// given
		ImageProcessingRequestedEvent requestedEvent = requestedEvent();
		MapRecord<String, String, String> message = message(requestedEvent);
		given(redisTemplate.opsForStream()).willReturn(streamOperations);

		// when
		listener.handle(message);

		// then
		ArgumentCaptor<ImageProcessingResultEvent> eventCaptor =
				ArgumentCaptor.forClass(ImageProcessingResultEvent.class);
		InOrder inOrder = inOrder(resultPublisher, streamOperations);
		inOrder.verify(resultPublisher, times(2)).publish(eventCaptor.capture());
		inOrder.verify(streamOperations).acknowledge(REQUEST_STREAM, REQUEST_GROUP, RECORD_ID);

		assertThat(eventCaptor.getAllValues())
				.extracting(ImageProcessingResultEvent::status)
				.containsExactly(ImageProcessingStatus.SCANNING, ImageProcessingStatus.SAFE);
		assertThat(eventCaptor.getAllValues())
				.allSatisfy(event -> {
					assertThat(event.publicId()).isEqualTo(requestedEvent.publicId());
					assertThat(event.reason()).isEmpty();
					assertThat(event.processedAt()).isEqualTo(PROCESSED_AT);
				});
	}

	@Test
	@DisplayName("이미지 처리 결과 발행이 실패하면 request stream 메시지를 ack하지 않는다")
	void doesNotAcknowledgeMessageWhenPublishingResultFails() {
		// given
		ImageProcessingRequestedEvent requestedEvent = requestedEvent();
		MapRecord<String, String, String> message = message(requestedEvent);
		RuntimeException exception = new RuntimeException("publish failed");
		willAnswer(invocation -> {
			ImageProcessingResultEvent event = invocation.getArgument(0);
			if (event.status() == ImageProcessingStatus.SAFE) {
				throw exception;
			}
			return null;
		}).given(resultPublisher).publish(any(ImageProcessingResultEvent.class));

		// when & then
		assertThatThrownBy(() -> listener.handle(message))
				.isSameAs(exception);

		then(resultPublisher).should().publish(argThat(event ->
				event.status() == ImageProcessingStatus.SCANNING
		));
		then(resultPublisher).should().publish(argThat(event ->
				event.status() == ImageProcessingStatus.SAFE
		));
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
