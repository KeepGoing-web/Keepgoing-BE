package com.keepgoing.keepgoing.worker.application.service;

import com.keepgoing.keepgoing.worker.application.port.in.ImageProcessingUseCase;
import com.keepgoing.keepgoing.worker.application.port.out.PendingMessageClaimPort;
import com.keepgoing.keepgoing.worker.application.port.out.PendingMessageDispositionPort;
import com.keepgoing.keepgoing.worker.domain.ClaimedPendingMessage;
import com.keepgoing.keepgoing.worker.infrastructure.config.redis.WorkerRedisStreamProperties;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PendingMessageRecoveryService {

	private final PendingMessageClaimPort pendingMessageClaim;
	private final PendingMessageDispositionPort pendingMessageDisposition;
	private final ImageProcessingUseCase imageProcessing;
	private final WorkerRedisStreamProperties properties;

	public void recoverPendingMessages() {
		List<ClaimedPendingMessage> claimedPendingMessages = pendingMessageClaim.claimIdleMessages(
				Duration.ofMillis(properties.pendingIdleTimeout()),
				properties.pendingBatchSize()
		);

		for (ClaimedPendingMessage message : claimedPendingMessages) {
			handleMessage(message);
		}
	}

	private void handleMessage(ClaimedPendingMessage message) {
		if (message.isRetryExhausted(properties.maxRetries())) {
			pendingMessageDisposition.sendToDlq(message);
			return;
		}

		try {
			imageProcessing.process(message.toCommand());
			pendingMessageDisposition.acknowledge(message.getMessageId());
		} catch (Exception e) {
			log.warn("메시지 처리 실패, 재시도 예약: {}", message.getMessageId(), e);
			pendingMessageDisposition.scheduleRetry(message);
		}
	}
}
