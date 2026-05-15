package com.keepgoing.keepgoing.ai.service;

import com.keepgoing.keepgoing.activity.service.ActivityDashboardService;
import com.keepgoing.keepgoing.ai.domain.AiNoteIndex;
import com.keepgoing.keepgoing.ai.domain.AiNoteIndexStatus;
import com.keepgoing.keepgoing.ai.repository.AiNoteIndexRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class AiNoteIndexingRequestServiceTest {

	@Mock
	AiNoteIndexRepository aiNoteIndexRepository;

	@Mock
	ApplicationEventPublisher applicationEventPublisher;

	@Spy
	Clock clock = Clock.fixed(
			Instant.parse("2026-05-14T00:00:00Z"),
			ActivityDashboardService.KST
	);

	@InjectMocks
	AiNoteIndexingRequestService aiNoteIndexingRequestService;

	@Nested
	@DisplayName("재색인 요청")
	class RequestReindex {

		@Test
		@DisplayName("기존 인덱스가 없으면 PENDING 상태 인덱스를 생성하고 이벤트를 발생한다")
		void createsPendingIndexWhenNotExists() {
			// given
			Long noteId = 10L;
			Long authorId = 1L;

			given(aiNoteIndexRepository.findById(noteId)).willReturn(Optional.empty());
			given(aiNoteIndexRepository.save(ArgumentMatchers.any(AiNoteIndex.class)))
					.willAnswer(invocation -> invocation.getArgument(0));

			// when
			aiNoteIndexingRequestService.requestReindex(noteId, authorId);

			// then
			ArgumentCaptor<AiNoteIndex> captor = ArgumentCaptor.forClass(AiNoteIndex.class);
			verify(aiNoteIndexRepository).save(captor.capture());

			AiNoteIndex saved = captor.getValue();
			assertThat(saved.getNoteId()).isEqualTo(noteId);
			assertThat(saved.getAuthorId()).isEqualTo(authorId);
			assertThat(saved.getStatus()).isEqualTo(AiNoteIndexStatus.PENDING);
			assertThat(saved.getLastRequestedAt()).isEqualTo(
					LocalDateTime.ofInstant(clock.instant(), clock.getZone())
			);

			verify(applicationEventPublisher)
					.publishEvent(new AiNoteIndexRequestedEvent(noteId, authorId));
			verifyNoMoreInteractions(aiNoteIndexRepository, applicationEventPublisher);
		}

		@Test
		@DisplayName("기존 인덱스가 있으면 PENDING 상태로 갱신하고 이벤트를 발행한다")
		void marksExistingIndexPending() {
			// given
			Long noteId = 10L;
			Long authorId = 1L;
			LocalDateTime oldTime = LocalDateTime.of(2026, 5, 1, 10, 0);

			AiNoteIndex existing = AiNoteIndex.pending(noteId, authorId, oldTime);
			existing.markCompleted(oldTime.plusHours(1));

			given(aiNoteIndexRepository.findById(noteId)).willReturn(Optional.of(existing));
			given(aiNoteIndexRepository.save(existing)).willReturn(existing);

			// when
			aiNoteIndexingRequestService.requestReindex(noteId, authorId);

			// then
			assertThat(existing.getStatus()).isEqualTo(AiNoteIndexStatus.PENDING);
			assertThat(existing.getAuthorId()).isEqualTo(authorId);
			assertThat(existing.getLastRequestedAt()).isEqualTo(
					LocalDateTime.ofInstant(clock.instant(), clock.getZone())
			);
			assertThat(existing.getLastError()).isNull();

			verify(aiNoteIndexRepository).save(existing);
			verify(applicationEventPublisher)
					.publishEvent(new AiNoteIndexRequestedEvent(noteId, authorId));
			verifyNoMoreInteractions(aiNoteIndexRepository, applicationEventPublisher);
		}
	}
}
