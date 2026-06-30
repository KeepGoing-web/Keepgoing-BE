package com.keepgoing.keepgoing.worker.infrastructure.adapter.in.schedule;

import com.keepgoing.keepgoing.worker.application.service.PendingMessageRecoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PendingMessageRecoveryScheduler {

	private final PendingMessageRecoveryService recoveryService;

	@Scheduled(fixedDelayString = "${image-processing.streams.pending-interval:30000}")
	public void recover() {
		recoveryService.recoverPendingMessages();
	}
}
