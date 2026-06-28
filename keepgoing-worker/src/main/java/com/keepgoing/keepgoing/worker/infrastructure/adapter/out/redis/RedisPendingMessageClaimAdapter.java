package com.keepgoing.keepgoing.worker.infrastructure.adapter.out.redis;

import com.keepgoing.keepgoing.common.image.event.ImageProcessingRequestedEvent;
import com.keepgoing.keepgoing.worker.application.port.out.PendingMessageClaimPort;
import com.keepgoing.keepgoing.worker.domain.ClaimedPendingMessage;
import com.keepgoing.keepgoing.worker.infrastructure.config.redis.WorkerRedisStreamProperties;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisPendingMessageClaimAdapter implements PendingMessageClaimPort {

	private final StringRedisTemplate redisTemplate;
	private final WorkerRedisStreamProperties properties;

	@Override
	public List<ClaimedPendingMessage> claimIdleMessages(Duration minIdleTime, long batchSize) {

		// 1. XPENDING에서 PEL 조회
		PendingMessages pendingMessages = redisTemplate.opsForStream()
				.pending(
						properties.request(),
						Consumer.from(properties.requestGroup(), properties.requestConsumer()),
						Range.unbounded(),
						batchSize
				);

		// 2. idle 시간 기준으로 필터
		List<PendingMessage> idleEntries = pendingMessages.stream()
				.filter(msg -> msg.getElapsedTimeSinceLastDelivery().compareTo(minIdleTime) >= 0)
				.toList();

		// 3.XCLAIM
		return idleEntries.stream()
				.map(entry -> claimRecordsForPendingMessage(entry, minIdleTime))
				.flatMap(Collection::stream)
				.map(this::toClaimedMessage)
				.toList();
	}

	private List<MapRecord<String, String, String>> claimRecordsForPendingMessage(
			PendingMessage pendingMessage,
			Duration minIdleTime
	) {
		StreamOperations<String, String, String> ops = redisTemplate.opsForStream();
		return ops.claim(
				properties.request(),
				properties.requestGroup(),
				properties.requestConsumer(),
				minIdleTime,
				RecordId.of(pendingMessage.getIdAsString()));
	}

	private ClaimedPendingMessage toClaimedMessage(MapRecord<String, String, String> record) {
		ImageProcessingRequestedEvent event = ImageProcessingRequestedEvent.from(record.getValue());
		return new ClaimedPendingMessage(
				record.getId().toString(),
				event.publicId(),
				event.storageKey(),
				event.contentType(),
				event.fileSize(),
				event.requestedAt(),
				event.retryCount()
		);
	}
}
