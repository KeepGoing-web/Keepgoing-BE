package com.keepgoing.keepgoing.ai.service;

import com.keepgoing.keepgoing.ai.repository.AiNoteIndexUpsertRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiNoteIndexingRequestService {

	private final AiNoteIndexUpsertRepository aiNoteIndexUpsertRepository;
	private final ApplicationEventPublisher applicationEventPublisher;
	private final Clock clock;

	@Transactional
	public void requestReindex(Long noteId, Long authorId) {
		LocalDateTime now = LocalDateTime.now(clock);

		aiNoteIndexUpsertRepository.upsertPending(noteId, authorId, now);
		applicationEventPublisher.publishEvent(new AiNoteIndexRequestedEvent(noteId, authorId));
	}
}
