package com.keepgoing.keepgoing.worker.infrastructure.adapter.in.redis;

import com.keepgoing.keepgoing.common.image.event.ImageProcessingRequestedEvent;
import com.keepgoing.keepgoing.worker.application.port.in.ImageProcessingUseCase;
import com.keepgoing.keepgoing.worker.infrastructure.config.redis.WorkerRedisStreamProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisImageProcessingRequestListener {

	private final StringRedisTemplate redisTemplate;
	private final WorkerRedisStreamProperties properties;
	private final ImageProcessingUseCase useCase;

	public void handle(MapRecord<String, String, String> message) {
		ImageProcessingRequestedEvent event = ImageProcessingRequestedEvent.from(message.getValue());
		useCase.process(ImageProcessingRequestMapper.toCommand(event));

		redisTemplate.opsForStream()
				.acknowledge(properties.request(), properties.requestGroup(), message.getId());
	}
}
