package com.keepgoing.keepgoing.worker.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;

import com.keepgoing.keepgoing.worker.application.port.in.ImageProcessingCommand;
import com.keepgoing.keepgoing.worker.application.port.in.ImageProcessingUseCase;
import com.keepgoing.keepgoing.worker.application.port.out.PendingMessageClaimPort;
import com.keepgoing.keepgoing.worker.application.port.out.PendingMessageDispositionPort;
import com.keepgoing.keepgoing.worker.domain.ClaimedPendingMessage;
import com.keepgoing.keepgoing.worker.infrastructure.config.redis.WorkerRedisStreamProperties;
import com.keepgoing.keepgoing.worker.support.WorkerRedisStreamPropertiesFixture;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PendingMessageRecoveryServiceTest {

	@Mock
	PendingMessageClaimPort pendingMessageClaim;

	@Mock
	PendingMessageDispositionPort pendingMessageDisposition;

	@Mock
	ImageProcessingUseCase imageProcessing;

	WorkerRedisStreamProperties properties;

	PendingMessageRecoveryService service;

	@BeforeEach
	void setUp() {
		properties = WorkerRedisStreamPropertiesFixture.createDefault();
		service = new PendingMessageRecoveryService(
				pendingMessageClaim,
				pendingMessageDisposition,
				imageProcessing,
				properties
		);
	}

	@Test
	@DisplayName("retryCount가 maxRetries 이상이면 DLQ로 이동한다")
	void retryExhausted_sendsToDlq() {
		// given
		ClaimedPendingMessage msg = new ClaimedPendingMessage(
				"msg-1",
				UUID.randomUUID(),
				"key",
				"image/jpeg",
				1024L,
				Instant.now(),
				3
		);
		given(pendingMessageClaim.claimIdleMessages(any(), anyLong()))
				.willReturn(List.of(msg));

		// when
		service.recoverPendingMessages();

		// then
		then(pendingMessageDisposition).should().sendToDlq(msg);
		then(imageProcessing).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("retryCount가 maxRetries 미만이고 처리 성공하면 ACK한다")
	void retryAvailableAndProcessingSucceeds_acks() {
		// given
		ClaimedPendingMessage msg = new ClaimedPendingMessage(
				"msg-1",
				UUID.randomUUID(),
				"key",
				"image/jpeg",
				1024L,
				Instant.now(),
				0
		);
		given(pendingMessageClaim.claimIdleMessages(any(), anyLong()))
				.willReturn(List.of(msg));

		// when
		service.recoverPendingMessages();

		// then
		then(imageProcessing).should().process(msg.toCommand());
		then(pendingMessageDisposition).should().acknowledge(msg.getMessageId());
	}

	@Test
	@DisplayName("retryCount가 maxRetries 미만이고 처리 실패하면 재시도를 예약한다")
	void retryAvailableAndProcessingFails_schedulesRetry() {
		// given
		ClaimedPendingMessage msg = new ClaimedPendingMessage(
				"msg-1",
				UUID.randomUUID(),
				"key",
				"image/jpeg",
				1024L,
				Instant.now(),
				0
		);
		given(pendingMessageClaim.claimIdleMessages(any(), anyLong()))
				.willReturn(List.of(msg));
		willThrow(new RuntimeException("processing failed"))
				.given(imageProcessing)
				.process(any());

		// when
		service.recoverPendingMessages();

		// then
		then(pendingMessageDisposition).should().scheduleRetry(msg);
	}

	@Test
	@DisplayName("claim된 메시지가 없으면 아무 일도 일어나지 않는다")
	void noClaimedMessages_doesNothing() {
		// claim 결과가 empty list → 아무 호출 없음
		// given
		given(pendingMessageClaim.claimIdleMessages(any(), anyLong()))
				.willReturn(List.of());

		// when
		service.recoverPendingMessages();

		// then
		then(pendingMessageDisposition).shouldHaveNoInteractions();
		then(imageProcessing).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("재시도 중인 메시지가 여러 개면 각각 독립적으로 처리한다")
	void multipleMessages_eachHandledIndependently() {
		var exhaustedMsg = new ClaimedPendingMessage("m1", UUID.randomUUID(), "k1", "image/jpeg", 1L, Instant.now(), 3);
		var successMsg = new ClaimedPendingMessage("m2", UUID.randomUUID(), "k2", "image/jpeg", 1L, Instant.now(), 0);
		var failMsg = new ClaimedPendingMessage("m3", UUID.randomUUID(), "k3", "image/jpeg", 1L, Instant.now(), 1);

		given(pendingMessageClaim.claimIdleMessages(any(), anyLong()))
				.willReturn(List.of(exhaustedMsg, successMsg, failMsg));
		willAnswer(invocation -> {
					var cmd = (ImageProcessingCommand) invocation.getArgument(0);
					if (cmd.publicId().equals(failMsg.getPublicId())) {
						throw new RuntimeException("processing failed");
					}
					return null;
				})
				.given(imageProcessing).process(any());

		// when
		service.recoverPendingMessages();

		// then
		then(pendingMessageDisposition).should().sendToDlq(exhaustedMsg);
		then(pendingMessageDisposition).should().acknowledge(successMsg.getMessageId());
		then(pendingMessageDisposition).should().scheduleRetry(failMsg);
		then(imageProcessing).should(times(2)).process(any());   // success + fail
	}
}