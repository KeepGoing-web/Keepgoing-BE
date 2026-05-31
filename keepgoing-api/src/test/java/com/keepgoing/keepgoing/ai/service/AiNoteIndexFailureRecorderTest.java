package com.keepgoing.keepgoing.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.keepgoing.keepgoing.ai.domain.AiNoteIndex;
import com.keepgoing.keepgoing.ai.domain.AiNoteIndexStatus;
import com.keepgoing.keepgoing.ai.repository.AiNoteIndexRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiNoteIndexFailureRecorderTest {

	@Mock
	AiNoteIndexRepository aiNoteIndexRepository;

	@InjectMocks
	AiNoteIndexFailureRecorder aiNoteIndexFailureRecorder;

	@Test
	@DisplayName("인덱스가 있으면 FAILED 상태로 마킹한다")
	void marksFailedWhenIndexExists() {
		// given
		Long noteId = 10L;
		Long authorId = 1L;
		LocalDateTime requestedAt = LocalDateTime.of(2026, 5, 14, 9, 0);
		LocalDateTime failedAt = LocalDateTime.of(2026, 5, 14, 9, 1);

		AiNoteIndex index = AiNoteIndex.pending(noteId, authorId, requestedAt);

		given(aiNoteIndexRepository.findById(noteId))
				.willReturn(Optional.of(index));

		// when
		aiNoteIndexFailureRecorder.markFailed(noteId, failedAt, "db read fail");

		// then
		assertThat(index.getStatus()).isEqualTo(AiNoteIndexStatus.FAILED);
		assertThat(index.getLastProcessedAt()).isEqualTo(failedAt);
		assertThat(index.getLastError()).contains("db read fail");
		assertThat(index.getAttemptCount()).isEqualTo(1);
	}
}