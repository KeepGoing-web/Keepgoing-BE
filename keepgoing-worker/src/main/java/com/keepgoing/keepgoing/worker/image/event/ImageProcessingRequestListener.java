package com.keepgoing.keepgoing.worker.image.event;

import com.keepgoing.keepgoing.common.image.domain.ImageProcessingStatus;
import com.keepgoing.keepgoing.common.image.event.ImageProcessingRequestedEvent;
import com.keepgoing.keepgoing.common.image.event.ImageProcessingResultEvent;
import com.keepgoing.keepgoing.worker.global.redis.WorkerRedisStreamProperties;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImageProcessingRequestListener {

	private final Clock clock;
	private final StringRedisTemplate redisTemplate;
	private final WorkerRedisStreamProperties properties;
	private final ImageProcessingResultPublisher resultPublisher;

	public void handle(MapRecord<String, String, String> message) {
		ImageProcessingRequestedEvent event = ImageProcessingRequestedEvent.from(message.getValue());

		resultPublisher.publish(new ImageProcessingResultEvent(
				event.publicId(),
				ImageProcessingStatus.SCANNING,
				"",
				Instant.now(clock)
		));

		// dummy

		resultPublisher.publish(new ImageProcessingResultEvent(
				event.publicId(),
				ImageProcessingStatus.SAFE,
				"",
				Instant.now(clock)
		));

		redisTemplate.opsForStream()
				.acknowledge(properties.request(), properties.requestGroup(), message.getId());
	}
}
