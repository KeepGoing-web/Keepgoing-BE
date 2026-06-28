package com.keepgoing.keepgoing.ai.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.keepgoing.keepgoing.activity.service.ActivityDashboardService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import com.keepgoing.keepgoing.ai.repository.AiNoteIndexRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

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
		@DisplayName("PENDING 상태로 upsert하고 이벤트를 발행한다")
		void upsertsPendingIndexAndPublishesEvent() {
			// given
			Long noteId = 10L;
			Long authorId = 1L;
			LocalDateTime requestedAt = LocalDateTime.now(clock);

			// when
			aiNoteIndexingRequestService.requestReindex(noteId, authorId);

			// then
			verify(aiNoteIndexRepository).upsertPending(noteId, authorId, requestedAt);
			verify(applicationEventPublisher)
					.publishEvent(new AiNoteIndexRequestedEvent(noteId, authorId));
			verifyNoMoreInteractions(aiNoteIndexRepository, applicationEventPublisher);
		}
	}
}
