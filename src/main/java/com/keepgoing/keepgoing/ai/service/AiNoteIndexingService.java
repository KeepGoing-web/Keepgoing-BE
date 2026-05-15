package com.keepgoing.keepgoing.ai.service;

import com.keepgoing.keepgoing.ai.domain.AiNoteChunk;
import com.keepgoing.keepgoing.ai.domain.AiNoteIndex;
import com.keepgoing.keepgoing.ai.repository.AiNoteChunkRepository;
import com.keepgoing.keepgoing.ai.repository.AiNoteIndexRepository;
import com.keepgoing.keepgoing.ai.repository.AiNoteIndexingSourceView;
import com.keepgoing.keepgoing.note.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiNoteIndexingService {

	private final NoteRepository noteRepository;
	private final AiNoteIndexRepository aiNoteIndexRepository;
	private final AiNoteChunkRepository aiNoteChunkRepository;
	private final AiNoteChunker aiNoteChunker;
	private final Clock clock;

	@Transactional
	public void process(Long noteId) {
		AiNoteIndex index = aiNoteIndexRepository.findById(noteId)
				.orElseThrow(() -> new IllegalStateException("AI note index not found: " + noteId));

		LocalDateTime processedAt = LocalDateTime.now(clock);

		try {
			AiNoteIndexingSourceView source = noteRepository.findAiIndexingSourceById(noteId)
					.orElse(null);

			if (source == null || source.getDeletedAt() != null || !source.isAiCollectable()) {
				aiNoteChunkRepository.deleteByNoteId(noteId);
				index.markRemoved(processedAt);
				return;
			}

			List<String> chunks = aiNoteChunker.split(source.getTitle(), source.getContent());

			aiNoteChunkRepository.deleteByNoteId(noteId);
			aiNoteChunkRepository.saveAll(createChunks(source, chunks, processedAt));

			index.markCompleted(processedAt);
		} catch (RuntimeException exception) {
			index.markFailed(processedAt, exception.getMessage());
			log.error("AI note indexing failed. noteId={}" + noteId, exception);
			throw exception;
		}
	}

	private List<AiNoteChunk> createChunks(
			AiNoteIndexingSourceView source,
			List<String> chunks,
			LocalDateTime indexedAt
	) {
		List<AiNoteChunk> results = new ArrayList<>();

		for (int i = 0; i < chunks.size(); i++) {
			results.add(AiNoteChunk.create(
					source.getNoteId(),
					source.getAuthorId(),
					i,
					source.getTitle(),
					chunks.get(i),
					source.getUpdatedAt(),
					indexedAt
			));
		}

		return results;
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
}
