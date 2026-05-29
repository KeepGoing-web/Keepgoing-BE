package com.keepgoing.keepgoing.note.event;

import com.keepgoing.keepgoing.common.image.event.ImageProcessingRequestedEvent;
import com.keepgoing.keepgoing.global.redis.RedisStreamProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NoteImageProcessingRequestPublisher {

	private final StringRedisTemplate redisTemplate;
	private final RedisStreamProperties properties;

	public RecordId publish(ImageProcessingRequestedEvent event) {
		return redisTemplate.opsForStream()
				.add(properties.request(), event.toMap());
	}
}
