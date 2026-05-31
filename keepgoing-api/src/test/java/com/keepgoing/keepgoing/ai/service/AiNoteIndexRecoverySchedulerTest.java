package com.keepgoing.keepgoing.ai.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.keepgoing.keepgoing.ai.domain.AiNoteIndex;
import com.keepgoing.keepgoing.ai.domain.AiNoteIndexStatus;
import com.keepgoing.keepgoing.ai.repository.AiNoteIndexRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class AiNoteIndexRecoverySchedulerTest {

	@Mock
	AiNoteIndexRepository aiNoteIndexRepository;

	@Mock
	AiNoteIndexingService aiNoteIndexingService;

	@InjectMocks
	AiNoteIndexRecoveryScheduler aiNoteIndexRecoveryScheduler;

	@Test
	@DisplayName("재처리 대상 인덱스를 조회해 순서대로 처리한다")
	void processesRetryTargets() {
		// given
		AiNoteIndex pending = AiNoteIndex.pending(
				10L,
				1L,
				LocalDateTime.of(2026, 5, 14, 9, 0)
		);

		AiNoteIndex failed = AiNoteIndex.pending(
				20L,
				1L,
				LocalDateTime.of(2026, 5, 14, 9, 1)
		);

		failed.markFailed(
				LocalDateTime.of(2026, 5, 14, 9, 2),
				"index failed"
		);

		given(aiNoteIndexRepository.findRetryTargets(
				List.of(AiNoteIndexStatus.PENDING, AiNoteIndexStatus.FAILED),
				3,
				PageRequest.of(0, 50)
		)).willReturn(List.of(pending, failed));

		// when
		aiNoteIndexRecoveryScheduler.recoverRetryTargets();

		// then
		InOrder inOrder = inOrder(aiNoteIndexingService);
		inOrder.verify(aiNoteIndexingService).process(10L);
		inOrder.verify(aiNoteIndexingService).process(20L);

		verify(aiNoteIndexRepository).findRetryTargets(
				List.of(AiNoteIndexStatus.PENDING, AiNoteIndexStatus.FAILED),
				3,
				PageRequest.of(0, 50)
		);
		verifyNoMoreInteractions(aiNoteIndexRepository, aiNoteIndexingService);
	}

	@Test
	@DisplayName("재처리 중 일부 인덱스가 실패해도 다음 인덱스 처리를 계속한다")
	void continuesWhenOneTargetFails() {
		// given
		AiNoteIndex first = AiNoteIndex.pending(
				10L,
				1L,
				LocalDateTime.of(2026, 5, 14, 9, 0)
		);
		AiNoteIndex second = AiNoteIndex.pending(
				20L,
				1L,
				LocalDateTime.of(2026, 5, 14, 9, 1)
		);

		given(aiNoteIndexRepository.findRetryTargets(
				List.of(AiNoteIndexStatus.PENDING, AiNoteIndexStatus.FAILED),
				3,
				PageRequest.of(0, 50)
		)).willReturn(List.of(first, second));

		willThrow(new RuntimeException("index failed"))
				.given(aiNoteIndexingService)
				.process(10L);

		// when
		aiNoteIndexRecoveryScheduler.recoverRetryTargets();

		// then
		InOrder inOrder = inOrder(aiNoteIndexingService);
		inOrder.verify(aiNoteIndexingService).process(10L);
		inOrder.verify(aiNoteIndexingService).process(20L);

		verify(aiNoteIndexRepository).findRetryTargets(
				List.of(AiNoteIndexStatus.PENDING, AiNoteIndexStatus.FAILED),
				3,
				PageRequest.of(0, 50)
		);
		verifyNoMoreInteractions(aiNoteIndexRepository, aiNoteIndexingService);
	}
}