package com.keepgoing.keepgoing.worker.infrastructure.adapter.out.redis;

import com.keepgoing.keepgoing.worker.application.port.out.PendingMessageDispositionPort;
import com.keepgoing.keepgoing.worker.domain.ClaimedPendingMessage;
import com.keepgoing.keepgoing.worker.infrastructure.config.redis.WorkerRedisStreamProperties;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisPendingMessageDispositionAdapter implements PendingMessageDispositionPort {

	private final StringRedisTemplate redisTemplate;
	private final WorkerRedisStreamProperties properties;

	@Override
	public void acknowledge(String messageId) {
		redisTemplate.opsForStream()
				.acknowledge(properties.request(), properties.requestGroup(), messageId);
	}

	@Override
	public void sendToDlq(ClaimedPendingMessage message) {
		redisTemplate.opsForStream()
				.add(MapRecord.create(properties.dlq(), messageBody(message)));
		acknowledge(message.getMessageId());
	}

	@Override
	public void scheduleRetry(ClaimedPendingMessage message) {
		message.incrementRetryCount();
		redisTemplate.opsForStream()
				.add(MapRecord.create(properties.request(), messageBody(message)));
		acknowledge(message.getMessageId());
	}

	private Map<String, String> messageBody(ClaimedPendingMessage msg) {
		return Map.of(
				"publicId", msg.getPublicId().toString(),
				"storageKey", msg.getStorageKey(),
				"contentType", msg.getContentType(),
				"fileSize", Long.toString(msg.getFileSize()),
				"requestedAt", msg.getRequestedAt().toString(),
				"retryCount", Integer.toString(msg.getRetryCount())
		);
	}
}
