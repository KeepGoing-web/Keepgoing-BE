package com.keepgoing.keepgoing.ai.service;

import com.keepgoing.keepgoing.ai.repository.AiNoteIndexRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiNoteIndexingRequestService {

	private final AiNoteIndexRepository aiNoteIndexRepository;
	private final ApplicationEventPublisher applicationEventPublisher;
	private final Clock clock;

	@Transactional
	public void requestReindex(Long noteId, Long authorId) {
		LocalDateTime now = LocalDateTime.now(clock);

		aiNoteIndexRepository.upsertPending(noteId, authorId, now);
		applicationEventPublisher.publishEvent(new AiNoteIndexRequestedEvent(noteId, authorId));
	}
}
