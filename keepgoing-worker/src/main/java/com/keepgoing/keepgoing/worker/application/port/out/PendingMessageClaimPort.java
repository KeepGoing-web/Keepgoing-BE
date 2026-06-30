package com.keepgoing.keepgoing.worker.application.port.out;

import com.keepgoing.keepgoing.worker.domain.ClaimedPendingMessage;
import java.time.Duration;
import java.util.List;

public interface PendingMessageClaimPort {
	List<ClaimedPendingMessage> claimIdleMessages(Duration minIdleTime, long batchSize);
}
