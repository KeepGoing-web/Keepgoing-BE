package com.keepgoing.keepgoing.ai.service;

import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiNoteIndexingService {

	private final AiNoteIndexingProcessor aiNoteIndexingProcessor;
	private final AiNoteIndexFailureRecorder aiNoteIndexFailureRecorder;
	private final Clock clock;

	public void process(Long noteId) {
		try {
			aiNoteIndexingProcessor.processInTransaction(noteId);
		} catch (RuntimeException exception) {
			LocalDateTime failedAt = LocalDateTime.now(clock);
			aiNoteIndexFailureRecorder.markFailed(noteId, failedAt, failureMessage(exception));

			log.error("AI note indexing failed. noteId={}", noteId, exception);
			throw exception;
		}
	}

	@Component
	@RequiredArgsConstructor
	static class Listener {

		private final AiNoteIndexingService aiNoteIndexingService;

		@Async("aiIndexingExecutor")
		@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
		public void handle(AiNoteIndexRequestedEvent event) {
			aiNoteIndexingService.process(event.noteId());
		}
	}

	private String failureMessage(RuntimeException exception) {
		String message = exception.getMessage();

		if (message == null || message.isBlank()) {
			return exception.getClass().getSimpleName();
		}

		return message;
	}
}
