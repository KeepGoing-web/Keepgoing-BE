package com.keepgoing.keepgoing.ai.service;

import com.keepgoing.keepgoing.ai.domain.AiNoteIndex;
import com.keepgoing.keepgoing.ai.repository.AiNoteIndexRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AiNoteIndexingRequestService {

	private final AiNoteIndexRepository aiNoteIndexRepository;
	private final ApplicationEventPublisher applicationEventPublisher;
	private final Clock clock;

	@Transactional
	public void requestReindex(Long noteId, Long authorId) {
		LocalDateTime now = LocalDateTime.now(clock);

		AiNoteIndex index = aiNoteIndexRepository.findById(noteId)
				.map(existing -> {
					existing.markPending(authorId, now);
					return existing;
				})
				.orElseGet(() -> AiNoteIndex.pending(noteId, authorId, now));

		aiNoteIndexRepository.save(index);
		applicationEventPublisher.publishEvent(new AiNoteIndexRequestedEvent(noteId, authorId));
	}
}
