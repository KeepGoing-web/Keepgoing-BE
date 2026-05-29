package com.keepgoing.keepgoing.note.event;

import com.keepgoing.keepgoing.common.image.event.ImageProcessingResultEvent;
import com.keepgoing.keepgoing.global.redis.RedisStreamProperties;
import com.keepgoing.keepgoing.note.service.NoteImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NoteImageProcessingResultListener {

	private final StringRedisTemplate redisTemplate;
	private final RedisStreamProperties properties;
	private final NoteImageService noteImageService;

	public void handle(MapRecord<String, String, String> message) {
		var event = ImageProcessingResultEvent.from(message.getValue());

		noteImageService.applyProcessingResult(event);
		redisTemplate.opsForStream()
				.acknowledge(properties.result(), properties.resultGroup(), message.getId());
	}
}
