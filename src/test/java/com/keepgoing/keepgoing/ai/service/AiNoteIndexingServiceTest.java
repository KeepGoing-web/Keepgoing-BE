package com.keepgoing.keepgoing.ai.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.keepgoing.keepgoing.activity.service.ActivityDashboardService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiNoteIndexingServiceTest {

	@Mock
	AiNoteIndexingProcessor aiNoteIndexingProcessor;

	@Mock
	AiNoteIndexFailureRecorder aiNoteIndexFailureRecorder;

	@Spy
	Clock clock = Clock.fixed(
			Instant.parse("2026-05-14T00:00:00Z"),
			ActivityDashboardService.KST
	);

	@InjectMocks
	AiNoteIndexingService aiNoteIndexingService;

	@Test
	@DisplayName("인덱싱 처리가 성공하면 실패 상태를 기록하지 않는다")
	void doesNotRecordFailureWhenProcessorSucceeds() {
		// given
		Long noteId = 10L;

		// when
		aiNoteIndexingService.process(noteId);

		// then
		verify(aiNoteIndexingProcessor).processInTransaction(noteId);
		verify(aiNoteIndexFailureRecorder, never())
				.markFailed(
						org.mockito.ArgumentMatchers.any(),
						org.mockito.ArgumentMatchers.any(),
						org.mockito.ArgumentMatchers.any()
				);
	}

	@Test
	@DisplayName("인덱싱 처리 중 예외가 발생하면 FAILED 상태를 기록하고 예외를 다시 던진다")
	void recordsFailureAndRethrowsWhenProcessorFails() {
		// given
		Long noteId = 10L;
		RuntimeException exception = new RuntimeException("db read fail");

		willThrow(exception)
				.given(aiNoteIndexingProcessor)
				.processInTransaction(noteId);

		// when & then
		assertThatThrownBy(() -> aiNoteIndexingService.process(noteId))
				.isSameAs(exception);

		verify(aiNoteIndexFailureRecorder).markFailed(
				noteId,
				LocalDateTime.of(2026, 5, 14, 9, 0),
				"db read fail"
		);
	}
}
