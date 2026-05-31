package com.keepgoing.keepgoing.worker.infrastructure.redis.adapter.out;

import com.keepgoing.keepgoing.common.image.event.ImageProcessingResultEvent;
import com.keepgoing.keepgoing.worker.image.application.port.out.ImageProcessingResultPublisherPort;
import com.keepgoing.keepgoing.worker.infrastructure.redis.WorkerRedisStreamProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisImageProcessingResultPublisher implements ImageProcessingResultPublisherPort {

	private final StringRedisTemplate redisTemplate;
	private final WorkerRedisStreamProperties properties;

	public void publish(ImageProcessingResultEvent event) {
		redisTemplate.opsForStream()
				.add(properties.result(), event.toMap());
	}
}
