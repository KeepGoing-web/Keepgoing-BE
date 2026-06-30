package com.keepgoing.keepgoing.worker.application.port.out;

import com.keepgoing.keepgoing.worker.domain.ClaimedPendingMessage;

public interface PendingMessageDispositionPort {

	void acknowledge(String messageId);

	void sendToDlq(ClaimedPendingMessage message);

	void scheduleRetry(ClaimedPendingMessage message);

}
