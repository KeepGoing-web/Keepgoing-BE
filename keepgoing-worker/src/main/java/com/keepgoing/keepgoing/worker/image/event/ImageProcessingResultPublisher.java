package com.keepgoing.keepgoing.worker.image.event;

import com.keepgoing.keepgoing.common.image.event.ImageProcessingResultEvent;
import com.keepgoing.keepgoing.worker.global.redis.WorkerRedisStreamProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImageProcessingResultPublisher {

	private final StringRedisTemplate redisTemplate;
	private final WorkerRedisStreamProperties properties;

	public void publish(ImageProcessingResultEvent event) {
		redisTemplate.opsForStream()
				.add(properties.result(), event.toMap());
	}
}
