package com.keepgoing.keepgoing.note.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

import com.keepgoing.keepgoing.common.image.domain.ImageProcessingStatus;
import com.keepgoing.keepgoing.common.image.event.ImageProcessingResultEvent;
import com.keepgoing.keepgoing.global.redis.RedisStreamProperties;
import com.keepgoing.keepgoing.note.service.NoteImageService;
import java.time.Instant;
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
class NoteImageProcessingResultListenerTest {

	private static final String REQUEST_STREAM = "note-image-processing-requests";
	private static final String RESULT_STREAM = "note-image-processing-results";
	private static final String RESULT_GROUP = "keepgoing-api";
	private static final String RESULT_CONSUMER = "api-1";
	private static final RecordId RECORD_ID = RecordId.of("1715750400000-0");
	private static final Instant PROCESSED_AT = Instant.parse("2026-05-15T00:00:00Z");

	@Mock
	StringRedisTemplate redisTemplate;

	@Mock
	StreamOperations<String, Object, Object> streamOperations;

	@Mock
	NoteImageService noteImageService;

	RedisStreamProperties properties;
	NoteImageProcessingResultListener listener;

	@BeforeEach
	void setUp() {
		properties = new RedisStreamProperties(
				true,
				REQUEST_STREAM,
				RESULT_STREAM,
				RESULT_GROUP,
				RESULT_CONSUMER
		);
		listener = new NoteImageProcessingResultListener(redisTemplate, properties, noteImageService);
	}

	@Test
	@DisplayName("이미지 처리 결과를 반영한 뒤 result stream 메시지를 ack한다")
	void acknowledgesMessageAfterApplyingProcessingResult() {
		// given
		ImageProcessingResultEvent event = processingResultEvent(ImageProcessingStatus.SAFE);
		MapRecord<String, String, String> message = message(event);
		given(redisTemplate.opsForStream()).willReturn(streamOperations);

		// when
		listener.handle(message);

		// then
		ArgumentCaptor<ImageProcessingResultEvent> eventCaptor =
				ArgumentCaptor.forClass(ImageProcessingResultEvent.class);
		InOrder inOrder = inOrder(noteImageService, streamOperations);
		inOrder.verify(noteImageService).applyProcessingResult(eventCaptor.capture());
		inOrder.verify(streamOperations).acknowledge(RESULT_STREAM, RESULT_GROUP, RECORD_ID);

		ImageProcessingResultEvent appliedEvent = eventCaptor.getValue();
		assertThat(appliedEvent.publicId()).isEqualTo(event.publicId());
		assertThat(appliedEvent.status()).isEqualTo(event.status());
		assertThat(appliedEvent.reason()).isEqualTo(event.reason());
		assertThat(appliedEvent.processedAt()).isEqualTo(event.processedAt());
	}

	@Test
	@DisplayName("이미지 처리 결과 반영이 실패하면 result stream 메시지를 ack하지 않는다")
	void doesNotAcknowledgeMessageWhenApplyingProcessingResultFails() {
		// given
		ImageProcessingResultEvent event = processingResultEvent(ImageProcessingStatus.REJECTED);
		MapRecord<String, String, String> message = message(event);
		RuntimeException exception = new RuntimeException("apply failed");
		willThrow(exception).given(noteImageService)
				.applyProcessingResult(any(ImageProcessingResultEvent.class));

		// when & then
		assertThatThrownBy(() -> listener.handle(message))
				.isSameAs(exception);

		then(noteImageService).should().applyProcessingResult(any(ImageProcessingResultEvent.class));
		then(redisTemplate).should(never()).opsForStream();
		then(streamOperations).shouldHaveNoInteractions();
	}

	private static ImageProcessingResultEvent processingResultEvent(ImageProcessingStatus status) {
		return new ImageProcessingResultEvent(
				UUID.randomUUID(),
				status,
				status == ImageProcessingStatus.REJECTED ? "invalid image" : "",
				PROCESSED_AT
		);
	}

	private static MapRecord<String, String, String> message(ImageProcessingResultEvent event) {
		return MapRecord.create(RESULT_STREAM, event.toMap()).withId(RECORD_ID);
	}
}
